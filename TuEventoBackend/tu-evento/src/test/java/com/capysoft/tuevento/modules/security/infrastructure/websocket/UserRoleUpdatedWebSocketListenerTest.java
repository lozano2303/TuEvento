package com.capysoft.tuevento.modules.security.infrastructure.websocket;

import com.capysoft.tuevento.modules.security.domain.event.OrganizerApprovedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserRoleUpdatedWebSocketListener")
class UserRoleUpdatedWebSocketListenerTest {

    @Mock private SimpMessagingTemplate messagingTemplate;
    @InjectMocks private UserRoleUpdatedWebSocketListener listener;

    private static OrganizerApprovedEvent event() {
        return OrganizerApprovedEvent.builder().organizerPetitionId(4).userId(7).alias("bob").build();
    }

    @Test
    @DisplayName("solicitud aprobada → ROLE_UPDATED con rol ORGANIZER")
    void organizerApproved_notifiesUser() {
        listener.onOrganizerApproved(event());

        verify(messagingTemplate).convertAndSend("/topic/users/7/session",
                Map.of("type", "ROLE_UPDATED", "role", "ORGANIZER"));
    }

    @Test
    @DisplayName("fallo del broker → no lanza excepción")
    void brokerFailure_isNonBreaking() {
        doThrow(new RuntimeException("Broker down"))
                .when(messagingTemplate).convertAndSend(anyString(), any(Object.class));

        assertThatNoException().isThrownBy(() -> listener.onOrganizerApproved(event()));
    }
}
