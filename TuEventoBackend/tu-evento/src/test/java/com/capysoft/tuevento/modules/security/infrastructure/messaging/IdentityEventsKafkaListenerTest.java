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

import com.capysoft.tuevento.modules.security.domain.event.OrganizerRoleRevokedEvent;
import com.capysoft.tuevento.modules.security.domain.event.UserActivatedEvent;
import com.capysoft.tuevento.shared.infrastructure.messaging.ProcessedEventStore;
import com.capysoft.tuevento.shared.infrastructure.outbox.OutboxHeaders;
import com.fasterxml.jackson.databind.ObjectMapper;

@DisplayName("IdentityEventsKafkaListener (identity-service → listeners del monolito)")
class IdentityEventsKafkaListenerTest {

    private final ProcessedEventStore processed = mock(ProcessedEventStore.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final TransactionTemplate tx = new TransactionTemplate() {
        @Override
        public void executeWithoutResult(java.util.function.Consumer<org.springframework.transaction.TransactionStatus> action) {
            action.accept(null);
        }
    };
    private final IdentityEventsKafkaListener listener =
            new IdentityEventsKafkaListener(processed, events, tx, new ObjectMapper());

    private static ConsumerRecord<String, String> record(String type, UUID eventId, String payload) {
        ConsumerRecord<String, String> rec = new ConsumerRecord<>("tuevento.user", 0, 0L, "7", payload);
        rec.headers().add(OutboxHeaders.EVENT_TYPE, type.getBytes(StandardCharsets.UTF_8));
        rec.headers().add(OutboxHeaders.EVENT_ID, eventId.toString().getBytes(StandardCharsets.UTF_8));
        return rec;
    }

    @Test
    @DisplayName("UserActivated de identity-service → evento de dominio, marcado como procesado")
    void republishesUserActivated() {
        UUID id = UUID.randomUUID();
        listener.onMessage(record("UserActivated", id,
                "{\"userId\":7,\"alias\":\"ana\",\"occurredAt\":\"2026-10-27T12:00:00\",\"origin\":\"identity-service\"}"));

        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(event.capture());
        assertThat(((UserActivatedEvent) event.getValue()).getAlias()).isEqualTo("ana");
        verify(processed).markProcessed(IdentityEventsKafkaListener.CONSUMER, id);
    }

    @Test
    @DisplayName("OrganizerRoleRevoked de identity-service → evento con quién revocó y el motivo")
    void republishesOrganizerRoleRevoked() {
        listener.onMessage(record("OrganizerRoleRevoked", UUID.randomUUID(),
                "{\"revocationId\":3,\"userId\":7,\"alias\":\"ana\",\"revokedBy\":1,\"reason\":\"Fraude reiterado\","
                        + "\"occurredAt\":\"2026-10-27T17:00:00Z\",\"origin\":\"identity-service\"}"));

        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(event.capture());
        OrganizerRoleRevokedEvent revoked = (OrganizerRoleRevokedEvent) event.getValue();
        assertThat(revoked.getUserId()).isEqualTo(7);
        assertThat(revoked.getRevokedBy()).isEqualTo(1);
        assertThat(revoked.getReason()).isEqualTo("Fraude reiterado");
    }

    @Test
    @DisplayName("los del propio monolito (sin origin), duplicados, de otro tipo o sin userId no se reenvían")
    void ignoresOthers() {
        UUID duplicated = UUID.randomUUID();
        when(processed.isProcessed(IdentityEventsKafkaListener.CONSUMER, duplicated)).thenReturn(true);
        listener.onMessage(record("OrganizerRoleRevoked", UUID.randomUUID(), "{\"userId\":7,\"revokedBy\":1}"));
        listener.onMessage(record("UserActivated", duplicated, "{\"userId\":7,\"origin\":\"identity-service\"}"));
        listener.onMessage(record("SeatsHeld", UUID.randomUUID(), "{\"userId\":7,\"origin\":\"identity-service\"}"));
        listener.onMessage(record("UserActivated", UUID.randomUUID(), "{\"alias\":\"x\",\"origin\":\"identity-service\"}"));

        verifyNoInteractions(events);
        verify(processed, never()).markProcessed(any(), any());
    }

    @Test
    @DisplayName("without an eventId it still publishes, and a missing or invalid occurredAt falls back to now")
    void withoutEventIdAndWithBadTime() {
        ConsumerRecord<String, String> noId = new ConsumerRecord<>("tuevento.user", 0, 0L, "7",
                "{\"userId\":7,\"occurredAt\":\"yesterday\"}");
        noId.headers().add(OutboxHeaders.EVENT_TYPE, "UserActivated".getBytes(StandardCharsets.UTF_8));
        listener.onMessage(noId);
        listener.onMessage(record("UserActivated", UUID.randomUUID(), "{\"userId\":8}"));

        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(events, org.mockito.Mockito.times(2)).publishEvent(event.capture());
        assertThat(event.getAllValues()).allSatisfy(published ->
                assertThat(((UserActivatedEvent) published).getOccurredAt()).isNotNull());
        verify(processed, org.mockito.Mockito.times(1)).markProcessed(any(), any());
    }

    @Test
    @DisplayName("an unreadable payload is discarded without publishing")
    void discardsUnreadable() {
        listener.onMessage(record("UserActivated", UUID.randomUUID(), "{not json"));

        verifyNoInteractions(events);
    }
}
