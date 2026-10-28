package com.capysoft.tuevento.modules.event.infrastructure.messaging;

import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.capysoft.tuevento.modules.event.application.port.in.CloseRevokedOrganizerEventsPort;
import com.capysoft.tuevento.shared.infrastructure.config.DomainEventsConsumerConfig;
import com.capysoft.tuevento.shared.infrastructure.messaging.ProcessedEventStore;
import com.capysoft.tuevento.shared.infrastructure.messaging.InboundEventRecords;
import com.capysoft.tuevento.shared.infrastructure.messaging.InboundEventRecords.Inbound;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Catálogo escucha {@code tuevento.user} y, ante {@code OrganizerRoleRevoked}, cierra los eventos
 * activos del organizador. Ignora los demás tipos de evento del topic.
 *
 * <p>Idempotente por el header {@code eventId}: un mensaje ya procesado se descarta. Si cerrar
 * algún evento falla, se lanza la excepción y el contenedor reintenta con backoff (ver
 * {@link DomainEventsConsumerConfig}); los eventos que ya se cerraron no se vuelven a tocar.
 * Un mensaje ilegible no se puede reintentar con éxito: se registra y se descarta.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "messaging.consumers.enabled", havingValue = "true", matchIfMissing = true)
public class OrganizerRoleRevokedKafkaListener {

    static final String TOPIC = "tuevento.user";
    static final String EVENT_TYPE = "OrganizerRoleRevoked";
    static final String CONSUMER = "catalogo.organizer-role-revoked";

    private final CloseRevokedOrganizerEventsPort closeRevokedOrganizerEventsPort;
    private final ProcessedEventStore             processedEventStore;
    private final ObjectMapper                    objectMapper;

    /** Lo que Catálogo lee del mensaje de Identidad; los demás campos se ignoran. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Message(Long revocationId, Integer userId, Integer revokedBy, String reason) {
    }

    @KafkaListener(topics = TOPIC, groupId = "catalogo-organizer-revoked",
            containerFactory = DomainEventsConsumerConfig.CONTAINER_FACTORY)
    public void onMessage(ConsumerRecord<String, String> rec) {
        Inbound<Message> inbound = InboundEventRecords
                .readNew(rec, EVENT_TYPE, CONSUMER, processedEventStore, objectMapper, Message.class)
                .orElse(null);
        if (inbound == null) {
            return;
        }
        UUID eventId = inbound.eventId();
        Message message = inbound.payload();
        if (message.userId() == null) {
            log.error("Discarding OrganizerRoleRevoked without userId: offset={}, eventId={}", rec.offset(), eventId);
            return;
        }

        closeRevokedOrganizerEventsPort.close(message.userId(), message.revokedBy(), message.reason());

        // Sin eventId no se puede recordar, pero cerrar eventos ya cerrados no hace nada
        if (eventId != null) {
            processedEventStore.markProcessed(CONSUMER, eventId);
        }
    }
}
