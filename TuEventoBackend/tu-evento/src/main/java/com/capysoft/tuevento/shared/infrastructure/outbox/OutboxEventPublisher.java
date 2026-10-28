package com.capysoft.tuevento.shared.infrastructure.outbox;

import com.capysoft.tuevento.shared.domain.outbox.OutboxEvent;
import com.capysoft.tuevento.shared.domain.outbox.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

/**
 * Fachada para guardar eventos de dominio en el outbox.
 *
 * <p>Llamar a {@link #publish} dentro de la transacción de negocio. El
 * {@link OutboxRelayScheduler} los publicará a Kafka en un paso posterior.
 *
 * <h3>Convención de topics</h3>
 * <p>Un topic por tipo de agregado y el tipo de evento en el header {@link OutboxHeaders#EVENT_TYPE}:
 * <pre>
 *   tuevento.{aggregateType}
 *   ej: tuevento.order   (OrderCreated, SeatsHeld, OrderPaid…)
 *       tuevento.event   (EventStatusChanged…)
 * </pre>
 * Todos los eventos de un agregado comparten topic y key ({@code aggregateId}), así que caen en
 * la misma partición y el consumidor los lee en el orden en que se publicaron.
 *
 * <h3>Orden por agregado</h3>
 * <p>El relay publica en orden de {@code created_at} dentro de cada agregado. Ese orden solo es el
 * real si quien publica serializa las escrituras del agregado: bloquear su fila (p. ej.
 * {@code SELECT ... FOR UPDATE} sobre la orden) antes de llamar a {@link #publish}. Dos
 * transacciones concurrentes sobre el mismo agregado sin ese bloqueo pueden confirmarse en un
 * orden distinto al de su {@code created_at}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private static final String TOPIC_PREFIX = "tuevento";

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    /**
     * Serializa {@code payload} como JSON y lo guarda en el outbox.
     *
     * @param aggregateType tipo del agregado (ej: "seat", "order")
     * @param aggregateId   ID del agregado como String
     * @param eventType     nombre del evento (ej: "SeatsHeld", "OrderPaid")
     * @param payload       objeto a serializar como JSON
     */
    public void publish(String aggregateType, String aggregateId,
                        String eventType, Object payload) {
        publish(aggregateType, aggregateId, eventType, payload, UUID.randomUUID());
    }

    /**
     * Igual que {@link #publish(String, String, String, Object)} pero con un {@code eventId}
     * elegido por quien publica, p. ej. derivado del ID de la orden en la saga. La restricción
     * única de {@code event_id} impide guardar dos veces el mismo evento de negocio.
     *
     * <p>Un {@code eventId} repetido no se ignora: la restricción falla y en PostgreSQL deja abortada
     * la transacción de negocio. Usarlo solo cuando repetirlo sea un error, o comprobar antes si
     * el evento ya existe.
     *
     * @param eventId ID estable del evento; viaja en el header {@link OutboxHeaders#EVENT_ID}
     */
    public void publish(String aggregateType, String aggregateId,
                        String eventType, Object payload, UUID eventId) {
        if (eventId == null) {
            throw new IllegalArgumentException("Outbox eventId is required");
        }
        if (aggregateType == null || aggregateType.isBlank()) {
            throw new IllegalArgumentException("Outbox aggregateType is required");
        }
        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("Outbox eventType is required");
        }
        String normalizedType = aggregateType.trim().toLowerCase(Locale.ROOT);
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.error("OutboxEventPublisher: failed to serialize payload for {}/{}/{}: {}",
                    aggregateType, aggregateId, eventType, e.getMessage(), e);
            throw new IllegalArgumentException(
                    "Cannot serialize outbox event payload: " + e.getMessage(), e);
        }

        String topic = TOPIC_PREFIX + "." + normalizedType;

        OutboxEvent event = OutboxEvent.builder()
                .eventId(eventId)
                .aggregateType(normalizedType)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .payload(json)
                .topic(topic)
                .processed(false)
                .createdAt(LocalDateTime.now())
                .retryCount(0)
                .build();

        outboxEventRepository.save(event);

        log.debug("OutboxEvent saved: eventId={}, aggregateType={}, aggregateId={}, eventType={}, topic={}",
                eventId, normalizedType, aggregateId, eventType, topic);
    }
}
