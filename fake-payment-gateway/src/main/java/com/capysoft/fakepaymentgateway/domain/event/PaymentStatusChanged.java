package com.capysoft.fakepaymentgateway.domain.event;

import com.capysoft.fakepaymentgateway.domain.model.Money;
import com.capysoft.fakepaymentgateway.domain.model.PaymentStatus;

import java.time.Instant;

/**
 * Evento de dominio que representa un cambio de estado de pago.
 * Inmutable, con IDs primitivos para serialización en webhook.
 */
public record PaymentStatusChanged(
    String eventId,        // UUID estable por transición (para idempotencia)
    String paymentId,
    PaymentStatus status,
    Money money,
    Instant timestamp
) {
}
