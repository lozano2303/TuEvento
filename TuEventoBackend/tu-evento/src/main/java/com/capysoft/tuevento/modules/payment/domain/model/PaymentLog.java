package com.capysoft.tuevento.modules.payment.domain.model;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Registro de auditoría para cambios de estado en pagos.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentLog {
    private Long paymentLogId;
    private Long paymentId;
    private PaymentStatus oldStatus;
    private PaymentStatus newStatus;
    private LocalDateTime changedAt;
    private String reason;
}
