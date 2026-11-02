package com.capysoft.tuevento.modules.catalogbridge.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.capysoft.tuevento.modules.catalogbridge.messaging.CatalogEventsOutboxWriter.EventStatusChanged;
import com.capysoft.tuevento.modules.catalogbridge.messaging.CatalogEventsOutboxWriter.EventUpserted;
import com.capysoft.tuevento.modules.event.domain.event.EventCreatedEvent;
import com.capysoft.tuevento.modules.event.domain.event.EventStatusChangedEvent;
import com.capysoft.tuevento.modules.event.domain.event.EventUpdatedEvent;
import com.capysoft.tuevento.modules.event.domain.model.Event;
import com.capysoft.tuevento.modules.event.domain.model.EventStatus;
import com.capysoft.tuevento.modules.event.domain.repository.EventRepository;
import com.capysoft.tuevento.shared.infrastructure.outbox.OutboxEventPublisher;

@DisplayName("CatalogEventsOutboxWriter — lo que replica Comercio")
class CatalogEventsOutboxWriterTest {

    private static final LocalDate START = LocalDate.of(2026, 12, 1);

    private final EventRepository events = mock(EventRepository.class);
    private final OutboxEventPublisher outbox = mock(OutboxEventPublisher.class);
    private final CatalogEventsOutboxWriter writer = new CatalogEventsOutboxWriter(events, outbox);

    private void stored() {
        when(events.findById(3L)).thenReturn(Optional.of(Event.builder().eventId(3L).userId(7L).eventName("Concierto")
                .status(EventStatus.PUBLISHED).startDate(START).build()));
    }

    private EventUpserted publishedUpsert() {
        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(outbox).publish(eq("event"), eq("3"), eq("EventUpserted"), payload.capture());
        return (EventUpserted) payload.getValue();
    }

    @Test
    @DisplayName("crear un evento publica la fila")
    void created() {
        stored();

        writer.onCreated(EventCreatedEvent.builder().eventId(3L).build());

        EventUpserted upsert = publishedUpsert();
        assertThat(upsert.ownerId()).isEqualTo(7L);
        assertThat(upsert.status()).isEqualTo("PUBLISHED");
    }

    @Test
    @DisplayName("editarlo publica el nombre y la fecha aunque EventUpdatedEvent no los lleve")
    void updated() {
        stored();

        writer.onUpdated(EventUpdatedEvent.builder().eventId(3L).build());

        EventUpserted upsert = publishedUpsert();
        assertThat(upsert.name()).isEqualTo("Concierto");
        assertThat(upsert.startDate()).isEqualTo(START);
    }

    @Test
    @DisplayName("un evento sin estado se publica con estado nulo")
    void withoutStatus() {
        when(events.findById(3L)).thenReturn(Optional.of(Event.builder().eventId(3L).userId(7L).build()));

        writer.onCreated(EventCreatedEvent.builder().eventId(3L).build());

        assertThat(publishedUpsert().status()).isNull();
    }

    @Test
    @DisplayName("un cambio de estado lleva el estado anterior, el nuevo y los datos de la fila")
    void statusChanged() {
        stored();

        writer.onStatusChanged(EventStatusChangedEvent.builder().eventId(3L).oldStatus("PUBLISHED")
                .newStatus("CANCELLED").build());

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(outbox).publish(eq("event"), eq("3"), eq("EventStatusChanged"), payload.capture());
        EventStatusChanged change = (EventStatusChanged) payload.getValue();
        assertThat(change.oldStatus()).isEqualTo("PUBLISHED");
        assertThat(change.newStatus()).isEqualTo("CANCELLED");
        assertThat(change.ownerId()).isEqualTo(7L);
        assertThat(change.name()).isEqualTo("Concierto");
    }

    @Test
    @DisplayName("si la fila ya no existe no publica nada")
    void missing() {
        when(events.findById(3L)).thenReturn(Optional.empty());

        writer.onUpdated(EventUpdatedEvent.builder().eventId(3L).build());
        writer.onStatusChanged(EventStatusChangedEvent.builder().eventId(3L).build());

        verifyNoInteractions(outbox);
    }
}
