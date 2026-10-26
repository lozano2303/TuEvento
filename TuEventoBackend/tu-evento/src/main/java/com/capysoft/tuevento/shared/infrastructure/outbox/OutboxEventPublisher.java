package com.capysoft.tuevento.shared.infrastructure.outbox;

import com.capysoft.tuevento.shared.domain.outbox.OutboxEvent;
import com.capysoft.tuevento.shared.domain.outbox.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Fachada para guardar eventos de dominio en el outbox.
 *
 * <p>Llamar a {@link #publish} dentro de la transacción de negocio. El
 * {@link OutboxRelayScheduler} los publicará a Kafka en un paso posterior.
 *
 * <h3>Convención de topics</h3>
 * <pre>
 *   tuevento.{aggregateType}.{eventType-kebab-case}
 *   ej: tuevento.seat.seats-held
 *       tuevento.order.order-paid
 *       tuevento.event.event-status-changed
 * </pre>
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
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.error("OutboxEventPublisher: failed to serialize payload for {}/{}/{}: {}",
                    aggregateType, aggregateId, eventType, e.getMessage(), e);
            throw new IllegalArgumentException(
                    "Cannot serialize outbox event payload: " + e.getMessage(), e);
        }

        String topic = buildTopic(aggregateType, eventType);

        OutboxEvent event = OutboxEvent.builder()
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .payload(json)
                .topic(topic)
                .processed(false)
                .createdAt(LocalDateTime.now())
                .retryCount(0)
                .build();

        outboxEventRepository.save(event);

        log.debug("OutboxEvent saved: aggregateType={}, aggregateId={}, eventType={}, topic={}",
                aggregateType, aggregateId, eventType, topic);
    }

    /**
     * Convierte "SeatsHeld" → "tuevento.seat.seats-held".
     * CamelCase → kebab-case con prefijo tuevento.{aggregateType}.
     */
    private String buildTopic(String aggregateType, String eventType) {
        String kebab = eventType
                .replaceAll("([a-z])([A-Z])", "$1-$2")
                .toLowerCase();
        return TOPIC_PREFIX + "." + aggregateType.toLowerCase() + "." + kebab;
    }
}
