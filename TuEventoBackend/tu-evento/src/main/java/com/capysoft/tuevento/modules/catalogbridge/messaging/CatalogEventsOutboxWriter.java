package com.capysoft.tuevento.modules.catalogbridge.messaging;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.capysoft.tuevento.modules.event.domain.event.EventCreatedEvent;
import com.capysoft.tuevento.modules.event.domain.event.EventStatusChangedEvent;
import com.capysoft.tuevento.modules.event.domain.event.EventUpdatedEvent;
import com.capysoft.tuevento.modules.event.domain.model.Event;
import com.capysoft.tuevento.modules.event.domain.repository.EventRepository;
import com.capysoft.tuevento.shared.infrastructure.outbox.OutboxEventPublisher;

import lombok.RequiredArgsConstructor;

/**
 * Publica en {@code tuevento.event} los cambios de un evento que replica Comercio en event_ref.
 * Corre antes del commit para que el mensaje se guarde en la misma transacción que el cambio, y lee
 * la fila ya actualizada: así el mensaje siempre lleva nombre y fecha aunque el evento de dominio no.
 *
 * <p>Borrar un evento no publica nada: solo se pueden borrar los DRAFT, que no tienen órdenes.
 */
@Component
@RequiredArgsConstructor
public class CatalogEventsOutboxWriter {

    static final String AGGREGATE = "event";
    static final String EVENT_UPSERTED = "EventUpserted";
    static final String EVENT_STATUS_CHANGED = "EventStatusChanged";

    private final EventRepository eventRepository;
    private final OutboxEventPublisher outboxEventPublisher;

    public record EventUpserted(Long eventId, Long ownerId, String name, String status, LocalDate startDate,
                                LocalDateTime updatedAt) {
    }

    public record EventStatusChanged(Long eventId, Long ownerId, String name, String oldStatus, String newStatus,
                                     LocalDate startDate, LocalDateTime occurredAt) {
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void onCreated(EventCreatedEvent event) {
        publishUpsert(event.getEventId());
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void onUpdated(EventUpdatedEvent event) {
        publishUpsert(event.getEventId());
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void onStatusChanged(EventStatusChangedEvent change) {
        eventRepository.findById(change.getEventId()).ifPresent(event ->
                outboxEventPublisher.publish(AGGREGATE, String.valueOf(event.getEventId()), EVENT_STATUS_CHANGED,
                        new EventStatusChanged(event.getEventId(), event.getUserId(), event.getEventName(),
                                change.getOldStatus(), change.getNewStatus(), event.getStartDate(),
                                LocalDateTime.now())));
    }

    private void publishUpsert(Long eventId) {
        eventRepository.findById(eventId).ifPresent((Event event) ->
                outboxEventPublisher.publish(AGGREGATE, String.valueOf(eventId), EVENT_UPSERTED,
                        new EventUpserted(eventId, event.getUserId(), event.getEventName(),
                                event.getStatus() == null ? null : event.getStatus().name(), event.getStartDate(),
                                LocalDateTime.now())));
    }
}
