package com.capysoft.tuevento.modules.wallet.application.usecase;

import com.capysoft.tuevento.modules.wallet.domain.model.*;
import com.capysoft.tuevento.modules.wallet.domain.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Libera una reserva PENDING cuando el pago por pasarela fue rechazado o falló.
 *
 * Transiciona PAYMENT PENDING → FAILED.
 * NO toca el balance (nunca se descontó).
 *
 * Invocado por ProcessWebhookUseCaseImpl en los casos DECLINED, FAILED, CANCELLED.
 *
 * Es IDEMPOTENTE: si ya está FAILED, no hace nada.
 */
@Service
@RequiredArgsConstructor
public class ReleaseWalletPaymentUseCaseImpl {

    private static final Logger log = LoggerFactory.getLogger(ReleaseWalletPaymentUseCaseImpl.class);

    private final WalletTransactionRepository walletTransactionRepository;

    @Transactional
    public void execute(Long transactionId) {
        WalletTransaction tx = walletTransactionRepository.findById(transactionId)
            .orElseThrow(() -> new IllegalArgumentException(
                "WalletTransaction not found: " + transactionId));

        // Idempotencia
        if (tx.getStatus() == WalletTransactionStatus.FAILED) {
            log.info("ReleaseWalletPayment idempotent skip: txId={}", transactionId);
            return;
        }

        WalletTransaction failed = tx.fail();
        walletTransactionRepository.save(failed);

        log.info("Wallet payment released (failed): txId={}, walletId={}, amount={}",
            transactionId, tx.getWalletId(), tx.getAmount());
    }
}
