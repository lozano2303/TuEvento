package com.capysoft.commerce.payment.infrastructure.persistence.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.capysoft.commerce.payment.domain.model.RefundStatus;

/**
 * Fila de reembolso + pago para el panel de admin (proyección de Spring Data: los alias de la
 * consulta deben coincidir con los getters). El nombre y el email del comprador no están aquí: son
 * de Identidad y se completan por su API. El nombre del evento es de Catálogo.
 */
public interface AdminRefundRow {
    Long getRefundId();
    Long getPaymentId();
    Long getOrderId();
    Long getUserId();
    Long getEventId();
    BigDecimal getGatewayAmount();
    BigDecimal getWalletAmount();
    String getCurrency();
    RefundStatus getStatus();
    String getReason();
    LocalDateTime getRequestedAt();
    LocalDateTime getProcessedAt();
}
