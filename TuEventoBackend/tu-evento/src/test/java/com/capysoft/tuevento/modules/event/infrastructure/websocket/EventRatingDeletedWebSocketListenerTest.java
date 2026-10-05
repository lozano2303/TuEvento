package com.capysoft.tuevento.modules.event.infrastructure.websocket;

import com.capysoft.tuevento.modules.event.domain.event.EventRatingDeletedEvent;
import org.junit.jupiter.api.DisplayName;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("EventRatingDeletedWebSocketListener")
class EventRatingDeletedWebSocketListenerTest {

    @Mock private SimpMessagingTemplate messagingTemplate;
    @InjectMocks private EventRatingDeletedWebSocketListener listener;

    private EventRatingDeletedEvent event() {
        return EventRatingDeletedEvent.builder()
                .ratingId(99L).eventId(42L).userId(7L)
                .occurredAt(LocalDateTime.now()).build();
    }

    @Test
    @DisplayName("publica al topic correcto con payload {ratingId}")
    void publishes_correct_topic_and_payload() {
        listener.onEventRatingDeleted(event());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> cap = ArgumentCaptor.forClass(Map.class);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/events/42/comments/deleted"), cap.capture());
        assertThat(cap.getValue()).containsOnlyKeys("ratingId");
        assertThat(cap.getValue().get("ratingId")).isEqualTo(99L);
    }

    @Test
    @DisplayName("fallo del broker → no lanza excepción")
    void broker_failure_non_breaking() {
        doThrow(new RuntimeException("Broker down"))
                .when(messagingTemplate).convertAndSend(anyString(), any(Object.class));

        assertThatNoException().isThrownBy(() -> listener.onEventRatingDeleted(event()));
    }
}
