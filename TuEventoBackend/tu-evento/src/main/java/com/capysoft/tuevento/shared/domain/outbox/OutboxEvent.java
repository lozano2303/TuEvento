package com.capysoft.tuevento.shared.domain.outbox;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Registro del patrón Transactional Outbox.
 *
 * <p>Se persiste en la misma transacción que el cambio de negocio que origina
 * el evento. El {@link com.capysoft.tuevento.shared.infrastructure.outbox.OutboxRelayScheduler}
 * lee las filas no procesadas y las publica a Kafka, garantizando entrega
 * at-least-once sin perder eventos aunque el proceso muera entre el commit de
 * BD y la publicación al broker.
 */
@Getter
@Builder
public class OutboxEvent {

    private Long id;

    /** Tipo del agregado que originó el evento (ej: "seat", "order", "event"). */
    private String aggregateType;

    /** ID del agregado como String (soporta Long, Integer, UUID). */
    private String aggregateId;

    /** Nombre del evento (ej: "SeatsHeld", "OrderPaid", "EventStatusChanged"). */
    private String eventType;

    /** JSON del payload del evento. */
    private String payload;

    /** Topic Kafka destino (ej: "tuevento.seat.seats-held"). */
    private String topic;

    private boolean processed;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;

    /** Número de intentos fallidos de publicación a Kafka. */
    private int retryCount;

    /** Último mensaje de error (para diagnóstico). */
    private String lastError;
}
