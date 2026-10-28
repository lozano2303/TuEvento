package com.capysoft.tuevento.modules.payment.application.dto.response;

import com.capysoft.tuevento.modules.payment.domain.model.RefundStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Fila del panel de reembolsos del admin (GET /admin/refunds): reembolso + pago + comprador +
 * evento. Mismos campos y nombres que antes en el JSON; alias, nombre y email del comprador los
 * aporta Identidad (nulos si no respondió).
 *
 * @param gatewayAmount parte pagada por pasarela
 * @param walletAmount  parte pagada con saldo de la wallet
 */
public record AdminRefundResponse(
        Long refundId,
        Long paymentId,
        Long orderId,
        Long userId,
        String alias,
        String fullName,
        String email,
        Long eventId,
        String eventName,
        BigDecimal gatewayAmount,
        BigDecimal walletAmount,
        String currency,
        RefundStatus status,
        String reason,
        LocalDateTime requestedAt,
        LocalDateTime processedAt) {
}
