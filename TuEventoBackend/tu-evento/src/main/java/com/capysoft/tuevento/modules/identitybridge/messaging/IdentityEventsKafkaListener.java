package com.capysoft.tuevento.modules.identitybridge.messaging;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.capysoft.tuevento.shared.domain.event.identity.OrganizerRoleRevokedEvent;
import com.capysoft.tuevento.shared.domain.event.identity.UserActivatedEvent;
import com.capysoft.tuevento.shared.infrastructure.config.DomainEventsConsumerConfig;
import com.capysoft.tuevento.shared.infrastructure.messaging.ProcessedEventStore;
import com.capysoft.tuevento.shared.infrastructure.messaging.InboundEventRecords;
import com.capysoft.tuevento.shared.infrastructure.messaging.InboundEventRecords.Inbound;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Las cuentas se activan y los roles de organizador se revocan en identity-service, que deja
 * {@code UserActivated} y {@code OrganizerRoleRevoked} en el outbox; el relay los publica en
 * {@code tuevento.user}. Aquí se vuelven a publicar como los eventos de dominio de siempre, dentro de
 * una transacción, para que los reciban los listeners AFTER_COMMIT del monolito sin cambiarlos (la
 * bienvenida, y la notificación y el ROLE_REVOKED por WebSocket de Notificaciones).
 *
 * <p>Solo se reenvían los de {@code origin = identity-service}: si el gateway vuelve al monolito, él
 * mismo revoca y ya publica el evento en memoria, y reenviarlo lo duplicaría. Idempotente por el
 * header {@code eventId}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "messaging.consumers.enabled", havingValue = "true", matchIfMissing = true)
public class IdentityEventsKafkaListener {

    static final String TOPIC = "tuevento.user";
    static final String USER_ACTIVATED = "UserActivated";
    static final String ORGANIZER_ROLE_REVOKED = "OrganizerRoleRevoked";
    static final Set<String> EVENT_TYPES = Set.of(USER_ACTIVATED, ORGANIZER_ROLE_REVOKED);
    static final String ORIGIN = "identity-service";
    static final String CONSUMER = "identidad.monolith-listeners";

    private final ProcessedEventStore processedEventStore;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Message(Integer userId, String alias, Integer revokedBy, String reason, String occurredAt, String origin) {
    }

    @KafkaListener(topics = TOPIC, groupId = "identidad-monolith-listeners",
            containerFactory = DomainEventsConsumerConfig.CONTAINER_FACTORY)
    public void onMessage(ConsumerRecord<String, String> rec) {
        Inbound<Message> inbound = InboundEventRecords
                .readNew(rec, EVENT_TYPES, CONSUMER, processedEventStore, objectMapper, Message.class)
                .orElse(null);
        if (inbound == null) {
            return;
        }
        String type = inbound.eventType();
        UUID eventId = inbound.eventId();
        Message message = inbound.payload();
        if (!ORIGIN.equals(message.origin())) {
            return;
        }
        if (message.userId() == null) {
            log.error("Discarding {} without userId: offset={}, eventId={}", type, rec.offset(), eventId);
            return;
        }
        Object event = USER_ACTIVATED.equals(type)
                ? UserActivatedEvent.builder().userId(message.userId()).alias(message.alias())
                        .occurredAt(parseTime(message.occurredAt())).build()
                : OrganizerRoleRevokedEvent.builder().userId(message.userId()).alias(message.alias())
                        .revokedBy(message.revokedBy()).reason(message.reason())
                        .occurredAt(parseTime(message.occurredAt())).build();
        // Marcar y publicar en la misma transacción: los listeners AFTER_COMMIT corren al confirmarla
        transactionTemplate.executeWithoutResult(status -> {
            if (eventId != null) {
                processedEventStore.markProcessed(CONSUMER, eventId);
            }
            eventPublisher.publishEvent(event);
        });
    }

    /** Acepta la fecha local (UserActivated) o el instante ISO en UTC (OrganizerRoleRevoked). */
    private static LocalDateTime parseTime(String value) {
        if (value == null) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.ofInstant(Instant.parse(value), ZoneId.systemDefault());
        } catch (Exception notAnInstant) {
            try {
                return LocalDateTime.parse(value);
            } catch (Exception e) {
                return LocalDateTime.now();
            }
        }
    }
}
