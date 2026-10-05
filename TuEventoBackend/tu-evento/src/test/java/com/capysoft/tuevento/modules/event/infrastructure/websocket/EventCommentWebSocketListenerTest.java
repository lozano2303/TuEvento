package com.capysoft.tuevento.modules.event.infrastructure.websocket;

import com.capysoft.tuevento.modules.event.domain.event.EventRatingAddedEvent;
import com.capysoft.tuevento.modules.profile.infrastructure.persistence.entity.ProfileEntity;
import com.capysoft.tuevento.modules.profile.infrastructure.persistence.repository.ProfileJpaRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link EventCommentWebSocketListener}.
 *
 * <h3>Coverage</h3>
 * <ul>
 *   <li>Happy path: publica al topic correcto con payload completo</li>
 *   <li>authorName cae a "Usuario" cuando no hay perfil</li>
 *   <li>Comentario no visible → no se publica</li>
 *   <li>Fallo de SimpMessagingTemplate → no lanza excepción (non-breaking)</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EventCommentWebSocketListener")
class EventCommentWebSocketListenerTest {

    @Mock private SimpMessagingTemplate  messagingTemplate;
    @Mock private ProfileJpaRepository   profileJpaRepository;

    @InjectMocks
    private EventCommentWebSocketListener listener;

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private static final Long EVENT_ID  = 42L;
    private static final Long USER_ID   = 7L;
    private static final Long RATING_ID = 99L;

    private EventRatingAddedEvent visibleEvent() {
        return EventRatingAddedEvent.builder()
                .ratingId(RATING_ID)
                .eventId(EVENT_ID)
                .userId(USER_ID)
                .rating(5)
                .comment("Increíble experiencia")
                .isVisible(true)
                .occurredAt(LocalDateTime.of(2026, 10, 5, 14, 30, 0))
                .build();
    }

    // ── Tests: happy path ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("Happy path")
    class HappyPath {

        @Test
        @DisplayName("publica al topic /topic/events/{id}/comments con payload completo")
        void publishes_to_correct_topic_with_full_payload() {
            ProfileEntity profile = mock(ProfileEntity.class);
            when(profile.getFullName()).thenReturn("Carlos López");
            when(profileJpaRepository.findByUserId(USER_ID.intValue()))
                    .thenReturn(Optional.of(profile));

            listener.onEventRatingAdded(visibleEvent());

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> payloadCaptor =
                    ArgumentCaptor.forClass(Map.class);
            verify(messagingTemplate).convertAndSend(
                    eq("/topic/events/" + EVENT_ID + "/comments"),
                    payloadCaptor.capture());

            Map<String, Object> payload = payloadCaptor.getValue();
            assertThat(payload.get("ratingId")).isEqualTo(RATING_ID);
            assertThat(payload.get("userId")).isEqualTo(USER_ID);
            assertThat(payload.get("authorName")).isEqualTo("Carlos López");
            assertThat(payload.get("rating")).isEqualTo(5);
            assertThat(payload.get("comment")).isEqualTo("Increíble experiencia");
            assertThat(payload.get("isVisible")).isEqualTo(true);
            assertThat(payload.get("createdAt")).isEqualTo("2026-10-05T14:30");
        }

        @Test
        @DisplayName("authorName cae a 'Usuario' cuando no hay perfil registrado")
        void authorName_fallback_when_no_profile() {
            when(profileJpaRepository.findByUserId(USER_ID.intValue()))
                    .thenReturn(Optional.empty());

            listener.onEventRatingAdded(visibleEvent());

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> payloadCaptor =
                    ArgumentCaptor.forClass(Map.class);
            verify(messagingTemplate).convertAndSend(anyString(), payloadCaptor.capture());

            assertThat(payloadCaptor.getValue().get("authorName")).isEqualTo("Usuario");
        }
    }

    // ── Tests: comentario no visible ──────────────────────────────────────────

    @Nested
    @DisplayName("Comentario no visible")
    class NotVisible {

        @Test
        @DisplayName("isVisible=false → no se llama a SimpMessagingTemplate")
        void non_visible_comment_is_not_broadcast() {
            EventRatingAddedEvent hiddenEvent = EventRatingAddedEvent.builder()
                    .ratingId(RATING_ID)
                    .eventId(EVENT_ID)
                    .userId(USER_ID)
                    .rating(1)
                    .comment("Comentario oculto")
                    .isVisible(false)
                    .occurredAt(LocalDateTime.now())
                    .build();

            listener.onEventRatingAdded(hiddenEvent);

            verifyNoInteractions(messagingTemplate);
            verifyNoInteractions(profileJpaRepository);
        }
    }

    // ── Tests: tolerancia a fallos ────────────────────────────────────────────

    @Nested
    @DisplayName("Tolerancia a fallos del WebSocket")
    class FaultTolerance {

        @Test
        @DisplayName("fallo de SimpMessagingTemplate → no lanza excepción (non-breaking)")
        void messaging_failure_does_not_propagate() {
            when(profileJpaRepository.findByUserId(USER_ID.intValue()))
                    .thenReturn(Optional.empty());
            doThrow(new RuntimeException("Broker no disponible"))
                    .when(messagingTemplate).convertAndSend(anyString(), any(Object.class));

            // No debe lanzar — el push WS es no-bloqueante
            assertThatNoException().isThrownBy(() ->
                    listener.onEventRatingAdded(visibleEvent()));
        }
    }
}
