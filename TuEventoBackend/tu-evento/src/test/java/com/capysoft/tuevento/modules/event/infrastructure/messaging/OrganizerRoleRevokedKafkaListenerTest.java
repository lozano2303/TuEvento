package com.capysoft.tuevento.modules.event.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.capysoft.tuevento.modules.event.application.port.in.CloseRevokedOrganizerEventsPort;
import com.capysoft.tuevento.shared.infrastructure.messaging.ProcessedEventStore;
import com.capysoft.tuevento.shared.infrastructure.outbox.OutboxHeaders;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrganizerRoleRevokedKafkaListener")
class OrganizerRoleRevokedKafkaListenerTest {

    private static final UUID EVENT_ID = UUID.fromString("0b3a39f2-6a1d-3c8e-9f5e-1d2c3b4a5f60");
    private static final String PAYLOAD = """
            {"revocationId":77,"userId":10,"alias":"john","revokedBy":1,
             "reason":"Incumplimiento de las normas","occurredAt":"2026-10-27T15:00:00Z","extra":"ignored"}""";

    @Mock CloseRevokedOrganizerEventsPort closePort;
    @Mock ProcessedEventStore processedEventStore;

    private OrganizerRoleRevokedKafkaListener listener() {
        return new OrganizerRoleRevokedKafkaListener(closePort, processedEventStore, new ObjectMapper());
    }

    private static ConsumerRecord<String, String> record(String eventType, String eventId, String payload) {
        ConsumerRecord<String, String> rec = new ConsumerRecord<>("tuevento.user", 0, 5L, "10", payload);
        if (eventType != null) {
            rec.headers().add(OutboxHeaders.EVENT_TYPE, eventType.getBytes(StandardCharsets.UTF_8));
        }
        if (eventId != null) {
            rec.headers().add(OutboxHeaders.EVENT_ID, eventId.getBytes(StandardCharsets.UTF_8));
        }
        return rec;
    }

    @Test
    @DisplayName("cierra los eventos del organizador y marca el mensaje como procesado")
    void closesAndMarks() {
        listener().onMessage(record("OrganizerRoleRevoked", EVENT_ID.toString(), PAYLOAD));

        verify(closePort).close(10, 1, "Incumplimiento de las normas");
        verify(processedEventStore).markProcessed(OrganizerRoleRevokedKafkaListener.CONSUMER, EVENT_ID);
    }

    @Test
    @DisplayName("un mensaje repetido (mismo eventId) se descarta")
    void duplicateIsSkipped() {
        when(processedEventStore.isProcessed(OrganizerRoleRevokedKafkaListener.CONSUMER, EVENT_ID)).thenReturn(true);

        listener().onMessage(record("OrganizerRoleRevoked", EVENT_ID.toString(), PAYLOAD));

        verifyNoInteractions(closePort);
        verify(processedEventStore, never()).markProcessed(any(), any());
    }

    @Test
    @DisplayName("si cerrar falla, la excepción sube para que el contenedor reintente y no se marca")
    void failureIsRetriedNotMarked() {
        when(closePort.close(10, 1, "Incumplimiento de las normas")).thenThrow(new IllegalStateException("db down"));

        assertThatThrownBy(() -> listener().onMessage(record("OrganizerRoleRevoked", EVENT_ID.toString(), PAYLOAD)))
                .isInstanceOf(IllegalStateException.class);
        verify(processedEventStore, never()).markProcessed(any(), any());
    }

    @Test
    @DisplayName("ignora los demás tipos de evento del topic y los mensajes sin tipo")
    void ignoresOtherEventTypes() {
        listener().onMessage(record("UserRegistered", EVENT_ID.toString(), "{}"));
        listener().onMessage(record(null, EVENT_ID.toString(), PAYLOAD));

        verifyNoInteractions(closePort, processedEventStore);
    }

    @Test
    @DisplayName("un mensaje ilegible o sin userId se descarta sin reintentar")
    void unreadableIsDiscarded() {
        listener().onMessage(record("OrganizerRoleRevoked", EVENT_ID.toString(), "not json"));
        listener().onMessage(record("OrganizerRoleRevoked", EVENT_ID.toString(), "{\"revocationId\":77}"));

        verifyNoInteractions(closePort);
        verify(processedEventStore, never()).markProcessed(any(), any());
    }

    @Test
    @DisplayName("sin eventId válido se procesa igual (cerrar es repetible) pero no se puede recordar")
    void withoutEventIdStillCloses() {
        listener().onMessage(record("OrganizerRoleRevoked", null, PAYLOAD));
        listener().onMessage(record("OrganizerRoleRevoked", "not-a-uuid", PAYLOAD));

        verify(closePort, org.mockito.Mockito.times(2)).close(10, 1, "Incumplimiento de las normas");
        verify(processedEventStore, never()).markProcessed(any(), any());
    }
}
