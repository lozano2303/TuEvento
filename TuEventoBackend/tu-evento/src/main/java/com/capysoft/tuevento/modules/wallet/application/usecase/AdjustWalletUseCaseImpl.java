package com.capysoft.tuevento.modules.wallet.application.usecase;

import com.capysoft.tuevento.modules.wallet.application.dto.WalletResponse;
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
import java.util.UUID;

/**
 * Ajuste manual de balance (solo ADMIN).
 *
 * El amount puede ser positivo (suma) o negativo (resta).
 * Nunca deja el balance negativo — lanza InsufficientWalletBalanceException si la resta
 * supera el balance actual.
 *
 * Flujo:
 *  1. Obtener wallet (la crea si no existe — caso de ajuste de crédito inicial)
 *  2. Calcular nuevo balance validando invariante >= 0
 *  3. Crear WalletTransaction ADJUSTMENT en COMPLETED con amount = abs(amount)
 *  4. Actualizar balance
 *  5. Crear WalletReference de trazabilidad (entityType=ORDER, entityId=0 para ajustes manuales)
 */
@Service
@RequiredArgsConstructor
public class AdjustWalletUseCaseImpl {

    private static final Logger log = LoggerFactory.getLogger(AdjustWalletUseCaseImpl.class);

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final WalletReferenceRepository walletReferenceRepository;

    @Transactional
    public WalletResponse execute(Long userId, BigDecimal amount, String reason, String createdBy) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Adjustment amount cannot be zero");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Reason is required for wallet adjustment");
        }

        // 1. Obtener o crear wallet
        Wallet wallet = walletRepository.findByUserId(userId)
            .orElseGet(() -> walletRepository.save(Wallet.builder()
                .userId(userId)
                .balance(BigDecimal.ZERO)
                .currency("COP")
                .version(0L)
                .build()));

        // 2. Calcular y validar nuevo balance
        BigDecimal absAmount = amount.abs();
        BigDecimal newBalance;
        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            newBalance = wallet.getBalance().add(absAmount);
        } else {
            newBalance = wallet.getBalance().subtract(absAmount);
            if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                throw new InsufficientWalletBalanceException(
                    "Adjustment would leave wallet balance negative: current=" +
                    wallet.getBalance() + ", adjustment=" + amount);
            }
        }

        // 3. Crear transacción ADJUSTMENT en COMPLETED
        WalletTransaction transaction = WalletTransaction.builder()
            .walletId(wallet.getWalletId())
            .type(WalletTransactionType.ADJUSTMENT)
            .amount(absAmount)
            .status(WalletTransactionStatus.COMPLETED)
            .balanceAfter(newBalance)
            .idempotencyKey("adj-" + UUID.randomUUID())
            .build();
        WalletTransaction savedTx = walletTransactionRepository.save(transaction);

        // 4. Actualizar balance
        Wallet adjusted = wallet.toBuilder().balance(newBalance).build();
        Wallet savedWallet = walletRepository.save(adjusted);

        // 5. Referencia de trazabilidad (ORDER/0 para ajustes manuales sin entidad específica)
        WalletReference reference = WalletReference.builder()
            .transactionId(savedTx.getTransactionId())
            .entityType(WalletReferenceEntityType.ORDER)
            .entityId(0L)
            .build();
        walletReferenceRepository.save(reference);

        log.info("Wallet adjusted: userId={}, amount={}, newBalance={}, by={}, reason={}",
            userId, amount, newBalance, createdBy, reason);

        BigDecimal pending = walletTransactionRepository.sumPendingPayments(savedWallet.getWalletId());
        return WalletResponse.fromDomain(savedWallet, pending);
    }
}
