package com.capysoft.tuevento.modules.wallet.domain.repository;

import com.capysoft.tuevento.modules.wallet.domain.model.WalletTransaction;
import com.capysoft.tuevento.modules.wallet.domain.model.WalletTransactionStatus;
import com.capysoft.tuevento.modules.wallet.domain.model.WalletTransactionType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida para persistencia de WalletTransaction.
 */
public interface WalletTransactionRepository {
    WalletTransaction save(WalletTransaction transaction);
    Optional<WalletTransaction> findById(Long transactionId);
    Optional<WalletTransaction> findByIdempotencyKey(String idempotencyKey);
    boolean existsByIdempotencyKey(String idempotencyKey);
    List<WalletTransaction> findByWalletId(Long walletId);

    /**
     * Suma de montos de transacciones PAYMENT en estado PENDING para una wallet.
     * Usado para calcular el saldo disponible (balance - pendingPayments).
     */
    BigDecimal sumPendingPayments(Long walletId);
}
