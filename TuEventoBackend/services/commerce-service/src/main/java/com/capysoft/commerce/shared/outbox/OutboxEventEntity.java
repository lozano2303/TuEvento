package com.capysoft.commerce.shared.outbox;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Fila del outbox (tabla outbox_event, la misma que usa el monolito). commerce-service solo
 * inserta eventos PENDING: los publica en Kafka el relay del monolito, con el reintento, el orden
 * por agregado y el estado FAILED de siempre. Solo se mapean las columnas que se escriben al crear.
 */
@Entity
@Table(name = "outbox_event")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, updatable = false)
    private UUID eventId;

    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "topic", nullable = false, length = 200)
    private String topic;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "processed", nullable = false)
    private boolean processed;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
