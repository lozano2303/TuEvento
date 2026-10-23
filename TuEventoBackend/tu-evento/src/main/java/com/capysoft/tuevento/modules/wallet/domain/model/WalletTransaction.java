package com.capysoft.tuevento.modules.wallet.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Movimiento de wallet. Representa una línea del libro de movimientos.
 *
 * INVARIANTES:
 * - amount siempre positivo (el signo lo da el type)
 * - Una vez creado, monto/tipo/referencias son inmutables
 * - Solo status puede cambiar: PENDING → COMPLETED | FAILED
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletTransaction {
    private Long transactionId;
    private Long walletId;
    private WalletTransactionType type;
    private BigDecimal amount;         // Siempre positivo
    private WalletTransactionStatus status;
    private BigDecimal balanceAfter;   // Snapshot de auditoría; null hasta COMPLETED
    private String idempotencyKey;     // UNIQUE — evita doble proceso
    private LocalDateTime createdAt;
    private String createdBy;

    /**
     * Retorna true si este movimiento suma al balance efectivo del wallet.
     * CREDIT, REVERSAL y ADJUSTMENT suman; PAYMENT resta.
     */
    public boolean isAdditive() {
        return type == WalletTransactionType.CREDIT
            || type == WalletTransactionType.REVERSAL
            || type == WalletTransactionType.ADJUSTMENT;
    }

    /**
     * Transiciona a COMPLETED y fija el snapshot de balance.
     * Solo válido desde PENDING.
     */
    public WalletTransaction complete(BigDecimal newBalance) {
        if (status != WalletTransactionStatus.PENDING) {
            throw new IllegalStateException(
                "Cannot complete wallet transaction " + transactionId + ": status is " + status);
        }
        return WalletTransaction.builder()
            .transactionId(this.transactionId)
            .walletId(this.walletId)
            .type(this.type)
            .amount(this.amount)
            .status(WalletTransactionStatus.COMPLETED)
            .balanceAfter(newBalance)
            .idempotencyKey(this.idempotencyKey)
            .createdAt(this.createdAt)
            .createdBy(this.createdBy)
            .build();
    }

    /**
     * Transiciona a FAILED sin tocar el balance.
     * Solo válido desde PENDING.
     */
    public WalletTransaction fail() {
        if (status != WalletTransactionStatus.PENDING) {
            throw new IllegalStateException(
                "Cannot fail wallet transaction " + transactionId + ": status is " + status);
        }
        return WalletTransaction.builder()
            .transactionId(this.transactionId)
            .walletId(this.walletId)
            .type(this.type)
            .amount(this.amount)
            .status(WalletTransactionStatus.FAILED)
            .balanceAfter(this.balanceAfter)
            .idempotencyKey(this.idempotencyKey)
            .createdAt(this.createdAt)
            .createdBy(this.createdBy)
            .build();
    }
}
