package com.capysoft.tuevento.modules.payment.domain.model;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Modelo de reembolso.
 * Estructura lista para fase futura (sin casos de uso implementados todavía).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Refund {
    private Long refundId;
    private Long paymentId;
    private RefundStatus status;
    private String reason;
    private LocalDateTime requestedAt;
    private LocalDateTime processedAt;
}
