package com.capysoft.commerce.ticket.infrastructure.messaging;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.capysoft.commerce.payment.application.port.in.RefundCancelledEventUseCase;
import com.capysoft.commerce.shared.messaging.DomainEventsConsumerConfig;
import com.capysoft.commerce.shared.messaging.InboundEventRecords;
import com.capysoft.commerce.shared.messaging.InboundEventRecords.Inbound;
import com.capysoft.commerce.shared.messaging.ProcessedEventStore;
import com.capysoft.commerce.ticket.infrastructure.persistence.entity.EventRefEntity;
import com.capysoft.commerce.ticket.infrastructure.persistence.repository.EventRefJpaRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Mantiene la réplica {@code event_ref} con lo que publica Catálogo en {@code tuevento.event} y, si
 * un evento se cancela, dispara sus reembolsos. Un reembolso ya hecho no se repite: solo se buscan
 * los pagos que siguen APPROVED.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "messaging.consumers.enabled", havingValue = "true", matchIfMissing = true)
public class CatalogEventsKafkaListener {

    static final String TOPIC = "tuevento.event";
    static final String EVENT_UPSERTED = "EventUpserted";
    static final String EVENT_STATUS_CHANGED = "EventStatusChanged";
    static final String CONSUMER = "comercio.catalog-events";
    static final String CANCELLED = "CANCELLED";

    private final EventRefJpaRepository eventRefRepository;
    private final RefundCancelledEventUseCase refundCancelledEvent;
    private final ProcessedEventStore processedEventStore;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;

    /** Lleva los campos de los dos tipos de evento: el tipo va en el header. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Message(Long eventId, Long ownerId, String name, String status, String newStatus, LocalDate startDate,
                   LocalDateTime updatedAt, LocalDateTime occurredAt) {
    }

    @KafkaListener(topics = TOPIC, groupId = "comercio-catalog-events",
            containerFactory = DomainEventsConsumerConfig.CONTAINER_FACTORY)
    public void onMessage(ConsumerRecord<String, String> rec) {
        Inbound<Message> inbound = InboundEventRecords.readNew(rec, Set.of(EVENT_UPSERTED, EVENT_STATUS_CHANGED),
                CONSUMER, processedEventStore, objectMapper, Message.class).orElse(null);
        if (inbound == null) {
            return;
        }
        Message message = inbound.payload();
        if (message.eventId() == null || message.ownerId() == null) {
            log.error("Discarding {} without eventId or ownerId: offset={}", inbound.eventType(), rec.offset());
            return;
        }
        boolean statusChange = EVENT_STATUS_CHANGED.equals(inbound.eventType());
        transactionTemplate.executeWithoutResult(tx -> upsert(message, statusChange));
        if (statusChange && CANCELLED.equals(message.newStatus())) {
            refundCancelledEvent.execute(message.eventId());
        }
        if (inbound.eventId() != null) {
            processedEventStore.markProcessed(CONSUMER, inbound.eventId());
        }
    }

    /** Un mensaje más viejo que lo guardado no pisa la réplica. */
    private void upsert(Message message, boolean statusChange) {
        LocalDateTime at = statusChange ? message.occurredAt() : message.updatedAt();
        LocalDateTime when = at != null ? at : LocalDateTime.now();
        boolean stale = eventRefRepository.findById(message.eventId())
                .map(stored -> stored.getUpdatedAt().isAfter(when))
                .orElse(false);
        if (stale) {
            return;
        }
        eventRefRepository.save(EventRefEntity.builder()
                .eventId(message.eventId())
                .ownerId(message.ownerId())
                .name(message.name())
                .status(statusChange ? message.newStatus() : message.status())
                .startDate(message.startDate())
                .updatedAt(when)
                .build());
    }
}
