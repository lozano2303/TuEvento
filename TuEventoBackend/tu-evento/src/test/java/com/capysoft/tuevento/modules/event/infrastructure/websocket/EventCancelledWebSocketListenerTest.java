package com.capysoft.tuevento.modules.event.infrastructure.websocket;

import com.capysoft.tuevento.modules.event.domain.event.EventStatusChangedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventCancelledWebSocketListener")
class EventCancelledWebSocketListenerTest {

    @Mock private SimpMessagingTemplate messagingTemplate;
    @InjectMocks private EventCancelledWebSocketListener listener;

    private static EventStatusChangedEvent event(Long eventId, String newStatus, String cause) {
        return EventStatusChangedEvent.builder()
                .eventId(eventId).oldStatus("PUBLISHED").newStatus(newStatus)
                .reason("Motivo interno del admin").cause(cause).build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> sentPayload() {
        ArgumentCaptor<Map<String, Object>> cap = ArgumentCaptor.forClass(Map.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/events/12/status"), cap.capture());
        return cap.getValue();
    }

    @Test
    @DisplayName("evento cancelado → EVENT_CANCELLED sin causa ni motivo")
    void cancelled_sendsPayloadWithoutCause() {
        listener.onEventStatusChanged(event(12L, "CANCELLED", null));

        assertThat(sentPayload())
                .containsOnlyKeys("type", "eventId")
                .containsEntry("type", "EVENT_CANCELLED")
                .containsEntry("eventId", 12L);
    }

    @Test
    @DisplayName("cancelado por revocación del organizador → incluye la causa, nunca el motivo")
    void cancelledByRevocation_includesCause() {
        listener.onEventStatusChanged(event(12L, "CANCELLED", "ORGANIZER_REVOKED"));

        assertThat(sentPayload())
                .containsEntry("cause", "ORGANIZER_REVOKED")
                .doesNotContainKey("reason");
    }

    @Test
    @DisplayName("evento cancelado → también se avisa en el canal global para quitarlo de los listados")
    void cancelled_broadcastsToListings() {
        listener.onEventStatusChanged(event(12L, "CANCELLED", "ORGANIZER_REVOKED"));

        verify(messagingTemplate).convertAndSend("/topic/events/cancelled",
                (Object) Map.of("type", "EVENT_CANCELLED", "eventId", 12L));
    }

    @Test
    @DisplayName("otro estado o sin eventId → no envía nada")
    void otherStatusOrMissingId_doesNothing() {
        listener.onEventStatusChanged(event(12L, "PUBLISHED", null));
        listener.onEventStatusChanged(event(null, "CANCELLED", null));

        verifyNoInteractions(messagingTemplate);
    }

    @Test
    @DisplayName("fallo del broker → no lanza excepción")
    void brokerFailure_isNonBreaking() {
        doThrow(new RuntimeException("Broker down"))
                .when(messagingTemplate).convertAndSend(anyString(), any(Object.class));

        assertThatNoException().isThrownBy(() ->
                listener.onEventStatusChanged(event(12L, "CANCELLED", null)));
    }
}
