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

import java.math.BigDecimal;

/**
 * Reserva saldo de la wallet para un pago (checkout).
 *
 * Crea un PAYMENT en PENDING — NO descuenta el balance todavía.
 * El saldo disponible (balance - PAYMENTs PENDING) sí se reduce, evitando
 * doble gasto concurrente gracias al @Version del Wallet aggregate.
 *
 * El balance físico solo se descuenta cuando ConfirmWalletPaymentUseCaseImpl
 * transiciona el PAYMENT a COMPLETED.
 *
 * @return transactionId de la reserva, para pasarlo a Confirm o Release.
 */
@Service
@RequiredArgsConstructor
public class ReserveWalletPaymentUseCaseImpl {

    private static final Logger log = LoggerFactory.getLogger(ReserveWalletPaymentUseCaseImpl.class);

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final WalletReferenceRepository walletReferenceRepository;

    @Transactional
    public Long execute(Long userId, BigDecimal amount, Long orderId) {
        Wallet wallet = walletRepository.findByUserId(userId)
            .orElseThrow(() -> new WalletNotFoundException(userId));

        // Calcular saldo disponible = balance - sum(PAYMENT PENDING)
        BigDecimal pending  = walletTransactionRepository.sumPendingPayments(wallet.getWalletId());
        BigDecimal available = wallet.getBalance().subtract(pending);

        if (available.compareTo(amount) < 0) {
            throw new InsufficientWalletBalanceException(
                "Insufficient available balance: available=" + available + ", requested=" + amount);
        }

        // Crear PAYMENT en PENDING — no tocar balance aún
        // Tocar la fila de wallet (forzar UPDATE de @Version) para bloqueo optimista
        // en reservas concurrentes: increment version without changing balance
        walletRepository.save(wallet.toBuilder().build());

        WalletTransaction transaction = WalletTransaction.builder()
            .walletId(wallet.getWalletId())
            .type(WalletTransactionType.PAYMENT)
            .amount(amount)
            .status(WalletTransactionStatus.PENDING)
            .balanceAfter(null) // Se setea al confirmar
            .idempotencyKey("reserve-" + orderId + "-" + wallet.getWalletId())
            .build();
        WalletTransaction savedTx = walletTransactionRepository.save(transaction);

        // Referencia a la orden
        walletReferenceRepository.save(WalletReference.builder()
            .transactionId(savedTx.getTransactionId())
            .entityType(WalletReferenceEntityType.ORDER)
            .entityId(orderId)
            .build());

        log.info("Wallet payment reserved: userId={}, amount={}, orderId={}, txId={}",
            userId, amount, orderId, savedTx.getTransactionId());

        return savedTx.getTransactionId();
    }
}
