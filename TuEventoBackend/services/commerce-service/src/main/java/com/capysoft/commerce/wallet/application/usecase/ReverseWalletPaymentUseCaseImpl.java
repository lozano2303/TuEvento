package com.capysoft.commerce.wallet.application.usecase;

import com.capysoft.commerce.wallet.domain.model.*;
import com.capysoft.commerce.wallet.domain.repository.WalletReferenceRepository;
import com.capysoft.commerce.wallet.domain.repository.WalletRepository;
import com.capysoft.commerce.wallet.domain.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Revierte un PAYMENT ya COMPLETED — devuelve el monto al balance del usuario.
 *
 * Usado cuando se reembolsa un pago que tenía crédito de wallet aplicado.
 * Crea una transacción REVERSAL en COMPLETED con el mismo monto que el PAYMENT original.
 *
 * Es IDEMPOTENTE: usa como idempotency key "reversal-{originalTransactionId}".
 * Si ya existe la REVERSAL, no crea una segunda.
 *
 * **FIX:** Si la wallet no existe (fue eliminada o nunca persistió), busca wallet por userId
 * derivado de las referencias de la transacción original. Si tampoco existe, crea la wallet.
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

        // Buscar wallet por walletId, si no existe intentar encontrarla/crearla
        Wallet wallet = walletRepository.findById(original.getWalletId())
            .orElseGet(() -> findOrCreateWalletForReversal(original));

        // Devolver el monto al balance
        Wallet credited = wallet.credit(original.getAmount());
        walletRepository.save(credited);

        // Crear REVERSAL en COMPLETED
        WalletTransaction reversal = WalletTransaction.builder()
            .walletId(wallet.getWalletId())
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

    /**
     * Busca o crea una wallet cuando la wallet original no existe.
     * Esto puede ocurrir si:
     * 1. La wallet fue eliminada manualmente
     * 2. La wallet nunca se persistió correctamente
     * 3. El walletId de la transacción quedó huérfano
     *
     * Estrategia:
     * 1. Buscar userId desde las referencias de la transacción original (ORDER)
     * 2. Buscar si existe una wallet para ese userId
     * 3. Si no existe, crear nueva wallet con balance = 0
     */
    private Wallet findOrCreateWalletForReversal(WalletTransaction original) {
        log.warn("Wallet {} not found during reversal, attempting recovery...", original.getWalletId());

        // Extraer userId desde las referencias ORDER
        Long userId = walletReferenceRepository.findByTransactionId(original.getTransactionId())
            .stream()
            .filter(ref -> ref.getEntityType() == WalletReferenceEntityType.ORDER)
            .findFirst()
            .map(WalletReference::getEntityId)
            .orElseThrow(() -> new IllegalStateException(
                "Cannot find ORDER reference for transaction " + original.getTransactionId() +
                " to determine userId for wallet recovery"));

        log.info("Found userId {} from ORDER reference, searching for existing wallet...", userId);

        // Buscar si el usuario ya tiene una wallet (podría haber sido recreada)
        return walletRepository.findByUserId(userId)
            .map(existingWallet -> {
                log.info("Found existing wallet {} for userId {}, using it for reversal",
                    existingWallet.getWalletId(), userId);
                return existingWallet;
            })
            .orElseGet(() -> {
                // Crear nueva wallet con balance = 0
                log.warn("No wallet found for userId {}, creating new wallet for refund credit", userId);
                Wallet newWallet = Wallet.builder()
                    .userId(userId)
                    .balance(BigDecimal.ZERO)
                    .currency("COP")
                    .build();
                return walletRepository.save(newWallet);
            });
    }
}
