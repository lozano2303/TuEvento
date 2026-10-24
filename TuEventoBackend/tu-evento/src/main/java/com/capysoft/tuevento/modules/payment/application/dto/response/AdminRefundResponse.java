package com.capysoft.tuevento.modules.payment.application.dto.response;

import com.capysoft.tuevento.modules.payment.domain.model.RefundStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Fila del panel de reembolsos del admin (GET /admin/refunds): reembolso + pago +
 * comprador + evento. Proyección de Spring Data: los alias de la consulta deben
 * coincidir con los getters.
 */
public interface AdminRefundResponse {
    Long getRefundId();
    Long getPaymentId();
    Long getOrderId();
    Long getUserId();
    String getAlias();
    String getFullName();
    String getEmail();
    Long getEventId();
    String getEventName();
    /** Parte pagada por pasarela. */
    BigDecimal getGatewayAmount();
    /** Parte pagada con saldo de la wallet. */
    BigDecimal getWalletAmount();
    String getCurrency();
    RefundStatus getStatus();
    String getReason();
    LocalDateTime getRequestedAt();
    LocalDateTime getProcessedAt();
}
