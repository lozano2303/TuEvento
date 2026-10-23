package com.capysoft.tuevento.modules.wallet.application.usecase;

import com.capysoft.tuevento.modules.wallet.domain.model.*;
import com.capysoft.tuevento.modules.wallet.domain.repository.WalletRepository;
import com.capysoft.tuevento.modules.wallet.domain.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Confirma un PAYMENT PENDING — descuenta el balance definitivamente.
 *
 * Invocado:
 *  - Por ProcessWebhookUseCaseImpl cuando llega payment.approved (pago mixto)
 *  - Por CreateWalletOnlyPaymentUseCase directamente (pago 100% wallet)
 *
 * Es IDEMPOTENTE: si el PAYMENT ya está en COMPLETED, no hace nada.
 */
@Service
@RequiredArgsConstructor
public class ConfirmWalletPaymentUseCaseImpl {

    private static final Logger log = LoggerFactory.getLogger(ConfirmWalletPaymentUseCaseImpl.class);

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    @Transactional
    public void execute(Long transactionId) {
        WalletTransaction tx = walletTransactionRepository.findById(transactionId)
            .orElseThrow(() -> new IllegalArgumentException(
                "WalletTransaction not found: " + transactionId));

        // Idempotencia — si ya está COMPLETED no hacer nada
        if (tx.getStatus() == WalletTransactionStatus.COMPLETED) {
            log.info("ConfirmWalletPayment idempotent skip: txId={}", transactionId);
            return;
        }

        Wallet wallet = walletRepository.findById(tx.getWalletId())
            .orElseThrow(() -> new WalletNotFoundException("Wallet not found: id=" + tx.getWalletId()));

        // Descontar balance (dominio valida que no quede negativo)
        Wallet debited = wallet.debit(tx.getAmount());
        walletRepository.save(debited);

        // Transicionar PAYMENT a COMPLETED con snapshot de balance
        WalletTransaction completed = tx.complete(debited.getBalance());
        walletTransactionRepository.save(completed);

        log.info("Wallet payment confirmed: txId={}, walletId={}, amount={}, newBalance={}",
            transactionId, tx.getWalletId(), tx.getAmount(), debited.getBalance());
    }
}
