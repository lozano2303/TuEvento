package com.capysoft.commerce.ticket.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import com.capysoft.commerce.payment.application.port.in.RefundCancelledEventUseCase;
import com.capysoft.commerce.shared.messaging.ProcessedEventStore;
import com.capysoft.commerce.shared.outbox.OutboxHeaders;
import com.capysoft.commerce.ticket.infrastructure.persistence.entity.EventRefEntity;
import com.capysoft.commerce.ticket.infrastructure.persistence.repository.EventRefJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@DisplayName("CatalogEventsKafkaListener — réplica event_ref y reembolsos")
class CatalogEventsKafkaListenerTest {

    private static final UUID EVENT_ID = UUID.randomUUID();
    private static final String UPSERT = """
            {"eventId":3,"ownerId":7,"name":"Concierto","status":"PUBLISHED","startDate":"2026-12-01","updatedAt":"2026-10-30T10:00:00"}""";
    private static final String CANCELLED = """
            {"eventId":3,"ownerId":7,"name":"Concierto","oldStatus":"PUBLISHED","newStatus":"CANCELLED","startDate":"2026-12-01","occurredAt":"2026-10-30T11:00:00"}""";

    private final EventRefJpaRepository repository = mock(EventRefJpaRepository.class);
    private final RefundCancelledEventUseCase refunds = mock(RefundCancelledEventUseCase.class);
    private final ProcessedEventStore processed = mock(ProcessedEventStore.class);
    private final CatalogEventsKafkaListener listener = new CatalogEventsKafkaListener(repository, refunds, processed,
            new TransactionTemplate(noopTransactions()), new ObjectMapper().registerModule(new JavaTimeModule()));

    private static PlatformTransactionManager noopTransactions() {
        PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
        when(manager.getTransaction(org.mockito.ArgumentMatchers.any())).thenReturn(new SimpleTransactionStatus());
        return manager;
    }

    private static ConsumerRecord<String, String> record(String type, String json) {
        ConsumerRecord<String, String> rec = new ConsumerRecord<>("tuevento.event", 0, 0L, "3", json);
        rec.headers().add(OutboxHeaders.EVENT_TYPE, type.getBytes(StandardCharsets.UTF_8));
        rec.headers().add(OutboxHeaders.EVENT_ID, EVENT_ID.toString().getBytes(StandardCharsets.UTF_8));
        return rec;
    }

    private EventRefEntity saved() {
        ArgumentCaptor<EventRefEntity> entity = ArgumentCaptor.forClass(EventRefEntity.class);
        verify(repository).save(entity.capture());
        return entity.getValue();
    }

    @Test
    @DisplayName("EventUpserted guarda la fila y marca el mensaje, sin reembolsar")
    void upsert() {
        when(repository.findById(3L)).thenReturn(Optional.empty());

        listener.onMessage(record("EventUpserted", UPSERT));

        EventRefEntity row = saved();
        assertThat(row.getOwnerId()).isEqualTo(7L);
        assertThat(row.getName()).isEqualTo("Concierto");
        assertThat(row.getStatus()).isEqualTo("PUBLISHED");
        assertThat(row.getStartDate()).isEqualTo(LocalDate.of(2026, 12, 1));
        verifyNoInteractions(refunds);
        verify(processed).markProcessed(CatalogEventsKafkaListener.CONSUMER, EVENT_ID);
    }

    @Test
    @DisplayName("una cancelación actualiza el estado y dispara los reembolsos")
    void cancelled() {
        when(repository.findById(3L)).thenReturn(Optional.empty());

        listener.onMessage(record("EventStatusChanged", CANCELLED));

        assertThat(saved().getStatus()).isEqualTo("CANCELLED");
        verify(refunds).execute(3L);
        verify(processed).markProcessed(CatalogEventsKafkaListener.CONSUMER, EVENT_ID);
    }

    @Test
    @DisplayName("otro cambio de estado no reembolsa")
    void otherStatus() {
        when(repository.findById(3L)).thenReturn(Optional.empty());

        listener.onMessage(record("EventStatusChanged", CANCELLED.replace("\"newStatus\":\"CANCELLED\"", "\"newStatus\":\"COMPLETED\"")));

        assertThat(saved().getStatus()).isEqualTo("COMPLETED");
        verifyNoInteractions(refunds);
    }

    @Test
    @DisplayName("un mensaje más viejo que lo guardado no pisa la réplica")
    void staleIgnored() {
        when(repository.findById(3L)).thenReturn(Optional.of(EventRefEntity.builder().eventId(3L).ownerId(7L)
                .updatedAt(LocalDateTime.of(2026, 10, 30, 12, 0)).build()));

        listener.onMessage(record("EventUpserted", UPSERT));

        verify(repository, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any());
        verify(processed).markProcessed(CatalogEventsKafkaListener.CONSUMER, EVENT_ID);
    }

    @Test
    @DisplayName("sin fecha usa la de ahora; descarta mensajes incompletos y de otros tipos")
    void incomplete() {
        when(repository.findById(3L)).thenReturn(Optional.empty());

        listener.onMessage(record("EventUpserted", "{\"eventId\":3,\"ownerId\":7}"));
        assertThat(saved().getUpdatedAt()).isNotNull();

        listener.onMessage(record("EventUpserted", "{\"eventId\":3}"));
        listener.onMessage(record("EventDeleted", UPSERT));
        verify(repository).save(org.mockito.ArgumentMatchers.any());
    }
}
