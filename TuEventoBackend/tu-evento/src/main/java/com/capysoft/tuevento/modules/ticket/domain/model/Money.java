package com.capysoft.tuevento.modules.ticket.domain.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Value object que representa dinero con monto y moneda.
 * Inmutable para garantizar integridad del dominio.
 */
@Getter
@AllArgsConstructor
@EqualsAndHashCode
public final class Money {
    private final BigDecimal amount;
    private final String currency;
    
    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add money with different currencies");
        }
        return new Money(this.amount.add(other.amount), this.currency);
    }
    
    @Override
    public String toString() {
        return amount + " " + currency;
    }
}
