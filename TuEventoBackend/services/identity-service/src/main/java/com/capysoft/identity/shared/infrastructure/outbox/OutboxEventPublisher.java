package com.capysoft.identity.shared.infrastructure.outbox;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/**
 * Guarda un evento en el outbox dentro de la transacción de negocio en curso: si la transacción
 * se deshace, el evento no existe. Mismo formato que el monolito (topic {@code tuevento.<agregado>},
 * eventId estable que viaja en el header).
 */
@Component
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private static final String TOPIC_PREFIX = "tuevento";

    private final OutboxEventJpaRepository repository;
    private final ObjectMapper objectMapper;
    private final Clock clock = Clock.systemDefaultZone();

    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(String aggregateType, String aggregateId, String eventType, Object payload, UUID eventId) {
        String type = aggregateType.trim().toLowerCase(Locale.ROOT);
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot serialize outbox event payload: " + e.getMessage(), e);
        }
        repository.save(OutboxEventEntity.builder()
                .eventId(eventId)
                .aggregateType(type)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .payload(json)
                .topic(TOPIC_PREFIX + "." + type)
                .status("PENDING")
                .processed(false)
                .retryCount(0)
                .createdAt(LocalDateTime.now(clock))
                .build());
    }
}
