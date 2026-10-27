package com.capysoft.tuevento.modules.security.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionTemplate;

import com.capysoft.tuevento.modules.security.domain.event.UserActivatedEvent;
import com.capysoft.tuevento.shared.infrastructure.messaging.ProcessedEventStore;
import com.capysoft.tuevento.shared.infrastructure.outbox.OutboxHeaders;
import com.fasterxml.jackson.databind.ObjectMapper;

@DisplayName("UserActivatedKafkaListener (identity-service → listeners del monolito)")
class UserActivatedKafkaListenerTest {

    private final ProcessedEventStore processed = mock(ProcessedEventStore.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final TransactionTemplate tx = new TransactionTemplate() {
        @Override
        public void executeWithoutResult(java.util.function.Consumer<org.springframework.transaction.TransactionStatus> action) {
            action.accept(null);
        }
    };
    private final UserActivatedKafkaListener listener =
            new UserActivatedKafkaListener(processed, events, tx, new ObjectMapper());

    private static ConsumerRecord<String, String> record(String type, UUID eventId, String payload) {
        ConsumerRecord<String, String> rec = new ConsumerRecord<>("tuevento.user", 0, 0L, "7", payload);
        rec.headers().add(OutboxHeaders.EVENT_TYPE, type.getBytes(StandardCharsets.UTF_8));
        rec.headers().add(OutboxHeaders.EVENT_ID, eventId.toString().getBytes(StandardCharsets.UTF_8));
        return rec;
    }

    @Test
    @DisplayName("UserActivated → evento de dominio con userId y alias, y queda marcado como procesado")
    void republishes() {
        UUID id = UUID.randomUUID();
        listener.onMessage(record("UserActivated", id, "{\"userId\":7,\"alias\":\"ana\",\"occurredAt\":\"2026-10-27T12:00:00\"}"));

        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(event.capture());
        UserActivatedEvent activated = (UserActivatedEvent) event.getValue();
        assertThat(activated.getUserId()).isEqualTo(7);
        assertThat(activated.getAlias()).isEqualTo("ana");
        verify(processed).markProcessed(UserActivatedKafkaListener.CONSUMER, id);
    }

    @Test
    @DisplayName("ya procesado, de otro tipo o sin userId → no publica nada")
    void ignoresDuplicatesAndOthers() {
        UUID id = UUID.randomUUID();
        when(processed.isProcessed(UserActivatedKafkaListener.CONSUMER, id)).thenReturn(true);
        listener.onMessage(record("UserActivated", id, "{\"userId\":7}"));
        listener.onMessage(record("OrganizerRoleRevoked", UUID.randomUUID(), "{\"userId\":7}"));
        listener.onMessage(record("UserActivated", UUID.randomUUID(), "{\"alias\":\"sin-id\"}"));

        verifyNoInteractions(events);
        verify(processed, never()).markProcessed(any(), any());
    }
}
