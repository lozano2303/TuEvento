package com.capysoft.tuevento.modules.notification.infrastructure.messaging;

import com.capysoft.tuevento.modules.notification.application.dto.SendNotificationCommand;
import com.capysoft.tuevento.modules.notification.application.usecase.SendNotificationUseCase;
import com.capysoft.tuevento.modules.notification.domain.model.NotificationTypeNames;
import com.capysoft.tuevento.modules.security.domain.event.OrganizerRoleRevokedEvent;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link OrganizerRoleRevokedListener}.
 *
 * <h3>Coverage</h3>
 * <ul>
 *   <li>Happy path: notification sent with ORGANIZER_ROLE_REVOKED type and correct userId</li>
 *   <li>Happy path: WebSocket message pushed to /topic/role-change/{userId} with ROLE_REVOKED type</li>
 *   <li>Resilience: exception in sendNotification is swallowed — no re-throw</li>
 *   <li>Resilience: exception in WS push is swallowed — no re-throw</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrganizerRoleRevokedListener")
class OrganizerRoleRevokedListenerTest {

    @Mock private SendNotificationUseCase sendNotificationUseCase;
    @Mock private SimpMessagingTemplate   messagingTemplate;

    @InjectMocks
    private OrganizerRoleRevokedListener listener;

    private static final Integer USER_ID   = 99;
    private static final String  USER_ALIAS = "alice_organizer";

    private OrganizerRoleRevokedEvent buildEvent() {
        return OrganizerRoleRevokedEvent.builder()
                .userId(USER_ID)
                .alias(USER_ALIAS)
                .occurredAt(LocalDateTime.of(2026, 10, 23, 10, 0, 0))
                .build();
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Happy path")
    class HappyPath {

        @Test
        @DisplayName("sends ORGANIZER_ROLE_REVOKED notification to the affected user")
        void sendNotification_withCorrectCommand() {
            listener.onOrganizerRoleRevoked(buildEvent());

            ArgumentCaptor<SendNotificationCommand> captor =
                    ArgumentCaptor.forClass(SendNotificationCommand.class);
            verify(sendNotificationUseCase).execute(captor.capture());

            SendNotificationCommand cmd = captor.getValue();
            assertThat(cmd.getTypeName()).isEqualTo(NotificationTypeNames.ORGANIZER_ROLE_REVOKED);
            assertThat(cmd.getUserIds()).containsExactly(USER_ID);
            assertThat(cmd.getEntityId()).isEqualTo(USER_ID.longValue());
            assertThat(cmd.getIdempotencySuffix()).isNotBlank();
        }

        @Test
        @DisplayName("pushes WebSocket ROLE_REVOKED payload to /topic/role-change/{userId}")
        void pushWebSocket_toCorrectDestination() {
            listener.onOrganizerRoleRevoked(buildEvent());

            String expectedDestination = "/topic/role-change/" + USER_ID;

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
            verify(messagingTemplate).convertAndSend(eq(expectedDestination), payloadCaptor.capture());

            Map<String, Object> payload = payloadCaptor.getValue();
            assertThat(payload.get("type")).isEqualTo("ROLE_REVOKED");
            assertThat(payload.get("newRole")).isEqualTo("USER");
            assertThat(payload.get("userId")).isEqualTo(USER_ID);
        }
    }

    // ── Resilience ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Resilience — exceptions must not propagate")
    class Resilience {

        @Test
        @DisplayName("exception in sendNotification is swallowed — method returns normally without re-throwing")
        void exceptionInNotification_isSwallowed() {
            doThrow(new RuntimeException("notification failure"))
                    .when(sendNotificationUseCase).execute(any());

            // Must not throw — the entire handler is wrapped in a try/catch
            listener.onOrganizerRoleRevoked(buildEvent());

            // The whole body is inside the try block; once it throws, execution
            // jumps to the catch and the WS push is not reached. No re-throw.
            verify(sendNotificationUseCase).execute(any());
        }

        @Test
        @DisplayName("exception in WS push is swallowed — method returns normally")
        void exceptionInWsPush_isSwallowed() {
            doThrow(new RuntimeException("ws failure"))
                    .when(messagingTemplate).convertAndSend(any(String.class), any(Object.class));

            // Must not throw
            listener.onOrganizerRoleRevoked(buildEvent());

            verify(sendNotificationUseCase).execute(any());
        }
    }
}
