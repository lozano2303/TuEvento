package com.capysoft.tuevento.modules.wallet.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Aggregate root del dominio wallet.
 *
 * INVARIANTES:
 * - balance nunca negativo (validado aquí + CHECK en BD)
 * - Una sola wallet por usuario (UNIQUE user_id en BD)
 * - @Version garantiza bloqueo optimista en concurrencia
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Wallet {
    private Long walletId;
    private Long userId;
    private BigDecimal balance;   // Valor cacheado; libro de movimientos es la fuente de verdad
    private String currency;      // Siempre "COP"
    private Long version;         // Bloqueo optimista JPA

    /**
     * Aplica un crédito al balance.
     * @param amount monto positivo a sumar
     */
    public Wallet credit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Credit amount must be positive");
        }
        return toBuilder().balance(this.balance.add(amount)).build();
    }

    /**
     * Descuenta el monto del balance al confirmar un PAYMENT.
     * Lanza InsufficientWalletBalanceException si quedaría negativo.
     */
    public Wallet debit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Debit amount must be positive");
        }
        BigDecimal newBalance = this.balance.subtract(amount);
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new InsufficientWalletBalanceException(
                "Insufficient wallet balance: available=" + this.balance + ", requested=" + amount);
        }
        return toBuilder().balance(newBalance).build();
    }

    /**
     * Builder con copia de todos los campos — permite toBuilder() en el aggregate.
     */
    public WalletBuilder toBuilder() {
        return Wallet.builder()
            .walletId(this.walletId)
            .userId(this.userId)
            .balance(this.balance)
            .currency(this.currency)
            .version(this.version);
    }
}
