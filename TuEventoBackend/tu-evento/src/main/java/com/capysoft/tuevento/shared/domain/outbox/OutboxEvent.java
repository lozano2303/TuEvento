package com.capysoft.tuevento.shared.domain.outbox;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

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

    /**
     * ID estable del evento de negocio. Se asigna al guardarlo, no cambia entre reintentos y
     * viaja en el header {@code eventId}: es la clave con la que el consumidor descarta duplicados.
     */
    private UUID eventId;

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

    private OutboxStatus status;

    /** true solo cuando se publicó; se mantiene por compatibilidad con la columna de la 106. */
    private boolean processed;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;

    /** Número de intentos fallidos de publicación a Kafka. */
    private int retryCount;

    /** Último mensaje de error (para diagnóstico). */
    private String lastError;

    /** Antes de esta fecha el relay no lo reintenta. */
    private LocalDateTime nextAttemptAt;

    private LocalDateTime lastAttemptAt;

    /** Momento en que agotó los reintentos y pasó a {@link OutboxStatus#FAILED}. */
    private LocalDateTime failedAt;
}
