package com.capysoft.commerce.ticket.application.dto.response;

import java.math.BigDecimal;

/**
 * Qué se puede reembolsar de una orden: solo los tickets PAID (no usados).
 *
 * @param amount           suma del precio de los tickets reembolsables
 * @param currency         moneda de la orden
 * @param refundableCount  tickets PAID (se reembolsan)
 * @param totalCount       tickets de la orden que no están cancelados
 */
public record RefundableTicketsSummary(BigDecimal amount, String currency, int refundableCount, int totalCount) {

    /** Ningún ticket reembolsable: todos se usaron o ya se reembolsaron. */
    public boolean isNothingToRefund() {
        return refundableCount == 0;
    }

    /** Solo una parte es reembolsable porque algún ticket ya se usó (o ya se reembolsó). */
    public boolean isPartial() {
        return refundableCount > 0 && refundableCount < totalCount;
    }
}
