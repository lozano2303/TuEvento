package com.capysoft.tuevento.modules.event.infrastructure.websocket;

import com.capysoft.tuevento.modules.event.domain.event.EventRatingDeletedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link EventRatingDeletedWebSocketListener}.
 *
 * <h3>Coverage</h3>
 * <ul>
 *   <li>Happy path: publica al topic correcto con payload { ratingId }</li>
 *   <li>Fallo de SimpMessagingTemplate → no lanza excepción (non-breaking)</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EventRatingDeletedWebSocketListener")
class EventRatingDeletedWebSocketListenerTest {

    @Mock private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private EventRatingDeletedWebSocketListener listener;

    private static final Long EVENT_ID  = 42L;
    private static final Long USER_ID   = 7L;
    private static final Long RATING_ID = 99L;

    private EventRatingDeletedEvent deletedEvent() {
        return EventRatingDeletedEvent.builder()
                .ratingId(RATING_ID)
                .eventId(EVENT_ID)
                .userId(USER_ID)
                .occurredAt(LocalDateTime.of(2026, 10, 5, 15, 0, 0))
                .build();
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Happy path")
    class HappyPath {

        @Test
        @DisplayName("publica al topic /topic/events/{id}/comments/deleted con payload { ratingId }")
        void publishes_to_correct_topic_with_rating_id_payload() {
            listener.onEventRatingDeleted(deletedEvent());

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> payloadCaptor =
                    ArgumentCaptor.forClass(Map.class);
            verify(messagingTemplate).convertAndSend(
                    eq("/topic/events/" + EVENT_ID + "/comments/deleted"),
                    payloadCaptor.capture());

            Map<String, Object> payload = payloadCaptor.getValue();
            assertThat(payload).containsOnlyKeys("ratingId");
            assertThat(payload.get("ratingId")).isEqualTo(RATING_ID);
        }
    }

    // ── Tolerancia a fallos ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Tolerancia a fallos del WebSocket")
    class FaultTolerance {

        @Test
        @DisplayName("fallo de SimpMessagingTemplate → no lanza excepción (non-breaking)")
        void messaging_failure_does_not_propagate() {
            doThrow(new RuntimeException("Broker no disponible"))
                    .when(messagingTemplate).convertAndSend(anyString(), any(Object.class));

            assertThatNoException().isThrownBy(() ->
                    listener.onEventRatingDeleted(deletedEvent()));
        }
    }
}
