package com.capysoft.tuevento.modules.wallet.application.usecase;

import com.capysoft.tuevento.modules.wallet.domain.model.*;
import com.capysoft.tuevento.modules.wallet.domain.repository.WalletReferenceRepository;
import com.capysoft.tuevento.modules.wallet.domain.repository.WalletRepository;
import com.capysoft.tuevento.modules.wallet.domain.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Revierte un PAYMENT ya COMPLETED — devuelve el monto al balance del usuario.
 *
 * Usado cuando se reembolsa un pago que tenía crédito de wallet aplicado.
 * Crea una transacción REVERSAL en COMPLETED con el mismo monto que el PAYMENT original.
 *
 * Es IDEMPOTENTE: usa como idempotency key "reversal-{originalTransactionId}".
 * Si ya existe la REVERSAL, no crea una segunda.
 *
 * Invocado por:
 *  - ProcessWebhookUseCaseImpl en el caso REFUNDED (si walletAmountApplied > 0)
 *  - RequestRefundUseCaseImpl en el path wallet-only
 */
@Service
@RequiredArgsConstructor
public class ReverseWalletPaymentUseCaseImpl {

    private static final Logger log = LoggerFactory.getLogger(ReverseWalletPaymentUseCaseImpl.class);

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final WalletReferenceRepository walletReferenceRepository;

    @Transactional
    public void execute(Long originalTransactionId, String reason) {
        String reversalKey = "reversal-" + originalTransactionId;

        // Idempotencia — no revertir dos veces el mismo PAYMENT
        if (walletTransactionRepository.existsByIdempotencyKey(reversalKey)) {
            log.info("ReverseWalletPayment idempotent skip: originalTxId={}", originalTransactionId);
            return;
        }

        WalletTransaction original = walletTransactionRepository.findById(originalTransactionId)
            .orElseThrow(() -> new IllegalArgumentException(
                "WalletTransaction not found: " + originalTransactionId));

        if (original.getStatus() != WalletTransactionStatus.COMPLETED) {
            throw new IllegalStateException(
                "Cannot reverse wallet transaction " + originalTransactionId +
                ": status is " + original.getStatus() + ", expected COMPLETED");
        }

        Wallet wallet = walletRepository.findById(original.getWalletId())
            .orElseThrow(() -> new WalletNotFoundException("Wallet not found: id=" + original.getWalletId()));

        // Devolver el monto al balance
        Wallet credited = wallet.credit(original.getAmount());
        walletRepository.save(credited);

        // Crear REVERSAL en COMPLETED
        WalletTransaction reversal = WalletTransaction.builder()
            .walletId(original.getWalletId())
            .type(WalletTransactionType.REVERSAL)
            .amount(original.getAmount())
            .status(WalletTransactionStatus.COMPLETED)
            .balanceAfter(credited.getBalance())
            .idempotencyKey(reversalKey)
            .build();
        WalletTransaction savedReversal = walletTransactionRepository.save(reversal);

        // Copiar las referencias del original para mantener la trazabilidad
        walletReferenceRepository.findByTransactionId(originalTransactionId)
            .forEach(ref -> walletReferenceRepository.save(WalletReference.builder()
                .transactionId(savedReversal.getTransactionId())
                .entityType(ref.getEntityType())
                .entityId(ref.getEntityId())
                .build()));

        log.info("Wallet payment reversed: originalTxId={}, reversalTxId={}, amount={}, newBalance={}, reason={}",
            originalTransactionId, savedReversal.getTransactionId(),
            original.getAmount(), credited.getBalance(), reason);
    }
}
