package com.capysoft.tuevento.modules.event.infrastructure.websocket;

import com.capysoft.tuevento.modules.event.domain.event.EventRatingAddedEvent;
import com.capysoft.tuevento.modules.event.application.port.out.UserDirectoryPort;
import com.capysoft.tuevento.modules.event.application.service.CommentAuthorAvatars;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link EventCommentWebSocketListener}.
 *
 * <h3>Coverage</h3>
 * <ul>
 *   <li>Payload con rating null (segundo comentario o organizador)</li>
 *   <li>Payload con isOrganizer=true</li>
 *   <li>Comentario no visible no se emite</li>
 *   <li>Fallo de SimpMessagingTemplate → non-breaking</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EventCommentWebSocketListener")
class EventCommentWebSocketListenerTest {

    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private UserDirectoryPort  userDirectoryPort;
    @Mock private CommentAuthorAvatars   commentAuthorAvatars;

    @InjectMocks
    private EventCommentWebSocketListener listener;

    private static final Long EVENT_ID  = 42L;
    private static final Long USER_ID   = 7L;
    private static final Long RATING_ID = 99L;

    private EventRatingAddedEvent event(Integer rating, Boolean isOrganizer) {
        return EventRatingAddedEvent.builder()
                .ratingId(RATING_ID).eventId(EVENT_ID).userId(USER_ID)
                .rating(rating).comment("Test comment").isVisible(true)
                .isOrganizer(isOrganizer)
                .occurredAt(LocalDateTime.of(2026, 10, 5, 14, 30, 0))
                .build();
    }

    @Nested
    @DisplayName("Happy path")
    class HappyPath {

        @Test
        @DisplayName("payload incluye rating null e isOrganizer=false para segundo comentario")
        void null_rating_and_false_organizer_in_payload() {
            when(userDirectoryPort.findUser(USER_ID.intValue())).thenReturn(Optional.empty());

            listener.onEventRatingAdded(event(null, false));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> cap = ArgumentCaptor.forClass(Map.class);
            verify(messagingTemplate).convertAndSend(
                    eq("/topic/events/" + EVENT_ID + "/comments"), cap.capture());

            Map<String, Object> payload = cap.getValue();
            assertThat(payload.get("rating")).isNull();
            assertThat(payload.get("isOrganizer")).isEqualTo(false);
            assertThat(payload.get("ratingId")).isEqualTo(RATING_ID);
        }

        @Test
        @DisplayName("payload incluye isOrganizer=true para el dueño del evento")
        void organizer_flag_propagated() {
            when(userDirectoryPort.findUser(USER_ID.intValue()))
                    .thenReturn(Optional.of(new UserDirectoryPort.UserRef(USER_ID.intValue(), "Carlos Org", null, null)));

            listener.onEventRatingAdded(event(null, true));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> cap = ArgumentCaptor.forClass(Map.class);
            verify(messagingTemplate).convertAndSend(anyString(), cap.capture());

            assertThat(cap.getValue().get("isOrganizer")).isEqualTo(true);
            assertThat(cap.getValue().get("authorName")).isEqualTo("Carlos Org");
            assertThat(cap.getValue().get("rating")).isNull();
        }

        @Test
        @DisplayName("authorName cae a 'Usuario' cuando no hay perfil")
        void author_name_fallback() {
            when(userDirectoryPort.findUser(USER_ID.intValue())).thenReturn(Optional.empty());

            listener.onEventRatingAdded(event(4, false));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> cap = ArgumentCaptor.forClass(Map.class);
            verify(messagingTemplate).convertAndSend(anyString(), cap.capture());
            assertThat(cap.getValue().get("authorName")).isEqualTo("Usuario");
        }

        @Test
        @DisplayName("payload incluye la foto de perfil del autor")
        void author_avatar_in_payload() {
            UserDirectoryPort.UserRef author = new UserDirectoryPort.UserRef(USER_ID.intValue(), "Carlos Org", null, 9);
            when(userDirectoryPort.findUser(USER_ID.intValue())).thenReturn(Optional.of(author));
            when(commentAuthorAvatars.of(author)).thenReturn("https://cdn/carlos.jpg");

            listener.onEventRatingAdded(event(4, false));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> cap = ArgumentCaptor.forClass(Map.class);
            verify(messagingTemplate).convertAndSend(anyString(), cap.capture());
            assertThat(cap.getValue().get("authorAvatarUrl")).isEqualTo("https://cdn/carlos.jpg");
        }
    }

    @Nested
    @DisplayName("Comentario no visible")
    class NotVisible {

        @Test
        @DisplayName("isVisible=false → no se llama a SimpMessagingTemplate")
        void hidden_comment_not_broadcast() {
            EventRatingAddedEvent hidden = EventRatingAddedEvent.builder()
                    .ratingId(RATING_ID).eventId(EVENT_ID).userId(USER_ID)
                    .rating(3).comment("Oculto").isVisible(false).isOrganizer(false)
                    .occurredAt(LocalDateTime.now()).build();

            listener.onEventRatingAdded(hidden);
            verifyNoInteractions(messagingTemplate, userDirectoryPort);
        }
    }

    @Nested
    @DisplayName("Tolerancia a fallos")
    class FaultTolerance {

        @Test
        @DisplayName("fallo de SimpMessagingTemplate → no lanza excepción")
        void messaging_failure_non_breaking() {
            when(userDirectoryPort.findUser(USER_ID.intValue())).thenReturn(Optional.empty());
            doThrow(new RuntimeException("Broker down"))
                    .when(messagingTemplate).convertAndSend(anyString(), any(Object.class));

            assertThatNoException().isThrownBy(() -> listener.onEventRatingAdded(event(null, false)));
        }
    }
}
