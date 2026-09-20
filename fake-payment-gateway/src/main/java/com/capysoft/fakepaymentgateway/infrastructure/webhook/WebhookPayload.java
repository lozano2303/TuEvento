package com.capysoft.fakepaymentgateway.infrastructure.webhook;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Payload del webhook enviado al backend principal.
 */
public record WebhookPayload(
    String eventId,
    String event,
    String paymentId,
    String status,
    BigDecimal amount,
    String currency,
    Instant timestamp
) {
}
