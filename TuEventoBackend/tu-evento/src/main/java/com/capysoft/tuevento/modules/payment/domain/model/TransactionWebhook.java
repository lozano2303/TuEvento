package com.capysoft.tuevento.modules.payment.domain.model;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Registro de auditoría de webhooks recibidos del gateway de pago.
 * El gatewayEventId garantiza idempotencia: eventos duplicados se descartan.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionWebhook {
    private Long webhookId;
    private Long paymentId;
    private String gatewayEventId; // Unique - para idempotencia
    private String payload; // JSON del webhook recibido
    private LocalDateTime receivedAt;
}
