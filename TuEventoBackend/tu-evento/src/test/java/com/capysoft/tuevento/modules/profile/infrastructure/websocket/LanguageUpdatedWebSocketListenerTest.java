package com.capysoft.tuevento.modules.profile.infrastructure.websocket;

import com.capysoft.tuevento.modules.profile.domain.event.ProfileUpdatedEvent;
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
@DisplayName("LanguageUpdatedWebSocketListener")
class LanguageUpdatedWebSocketListenerTest {

    @Mock private SimpMessagingTemplate messagingTemplate;
    @InjectMocks private LanguageUpdatedWebSocketListener listener;

    private static ProfileUpdatedEvent event(Integer userId, Long languageId) {
        return ProfileUpdatedEvent.builder().profileId(3L).userId(userId).changedLanguageId(languageId).build();
    }

    @Test
    @DisplayName("cambio de idioma → LANGUAGE_UPDATED con el languageId")
    void languageChanged_notifiesUser() {
        listener.onProfileUpdated(event(7, 2L));

        verify(messagingTemplate).convertAndSend("/topic/users/7/session",
                Map.of("type", "LANGUAGE_UPDATED", "languageId", 2L));
    }

    @Test
    @DisplayName("perfil sin cambio de idioma o sin usuario → no envía nada")
    void noLanguageChangeOrUser_doesNothing() {
        listener.onProfileUpdated(event(7, null));
        listener.onProfileUpdated(event(null, 2L));

        verifyNoInteractions(messagingTemplate);
    }

    @Test
    @DisplayName("fallo del broker → no lanza excepción")
    void brokerFailure_isNonBreaking() {
        doThrow(new RuntimeException("Broker down"))
                .when(messagingTemplate).convertAndSend(anyString(), any(Object.class));

        assertThatNoException().isThrownBy(() -> listener.onProfileUpdated(event(7, 2L)));
    }
}
