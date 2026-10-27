package com.capysoft.tuevento.modules.security.infrastructure.messaging;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.capysoft.tuevento.modules.security.domain.event.UserActivatedEvent;
import com.capysoft.tuevento.shared.infrastructure.config.DomainEventsConsumerConfig;
import com.capysoft.tuevento.shared.infrastructure.messaging.ProcessedEventStore;
import com.capysoft.tuevento.shared.infrastructure.outbox.OutboxHeaders;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Las cuentas ya se activan en identity-service, que deja {@code UserActivated} en el outbox; el
 * relay lo publica en {@code tuevento.user}. Aquí se vuelve a publicar como el evento de dominio de
 * siempre, dentro de una transacción, para que lo reciban los listeners AFTER_COMMIT del monolito
 * (la bienvenida de Notificaciones) sin cambiarlos. Idempotente por el header {@code eventId}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "messaging.consumers.enabled", havingValue = "true", matchIfMissing = true)
public class UserActivatedKafkaListener {

    static final String TOPIC = "tuevento.user";
    static final String EVENT_TYPE = "UserActivated";
    static final String CONSUMER = "identidad.user-activated";

    private final ProcessedEventStore processedEventStore;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Message(Integer userId, String alias, String occurredAt) {
    }

    @KafkaListener(topics = TOPIC, groupId = "identidad-user-activated",
            containerFactory = DomainEventsConsumerConfig.CONTAINER_FACTORY)
    public void onMessage(ConsumerRecord<String, String> rec) {
        if (!EVENT_TYPE.equals(header(rec, OutboxHeaders.EVENT_TYPE))) {
            return;
        }
        UUID eventId = parseEventId(header(rec, OutboxHeaders.EVENT_ID));
        if (eventId != null && processedEventStore.isProcessed(CONSUMER, eventId)) {
            log.debug("UserActivated {} already processed, skipping", eventId);
            return;
        }
        Message message;
        try {
            message = objectMapper.readValue(rec.value(), Message.class);
        } catch (Exception e) {
            log.error("Discarding unreadable UserActivated: offset={}, eventId={}: {}", rec.offset(), eventId,
                    e.getMessage());
            return;
        }
        if (message.userId() == null) {
            log.error("Discarding UserActivated without userId: offset={}, eventId={}", rec.offset(), eventId);
            return;
        }
        // Marcar y publicar en la misma transacción: los listeners AFTER_COMMIT corren al confirmarla
        transactionTemplate.executeWithoutResult(status -> {
            if (eventId != null) {
                processedEventStore.markProcessed(CONSUMER, eventId);
            }
            eventPublisher.publishEvent(UserActivatedEvent.builder()
                    .userId(message.userId())
                    .alias(message.alias())
                    .occurredAt(parseTime(message.occurredAt()))
                    .build());
        });
    }

    private static LocalDateTime parseTime(String value) {
        try {
            return value == null ? LocalDateTime.now() : LocalDateTime.parse(value);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }

    private static String header(ConsumerRecord<String, String> rec, String name) {
        Header header = rec.headers().lastHeader(name);
        return header == null || header.value() == null ? null : new String(header.value(), StandardCharsets.UTF_8);
    }

    private static UUID parseEventId(String value) {
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
