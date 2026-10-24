package com.capysoft.tuevento.modules.theme.infrastructure.websocket;

import com.capysoft.tuevento.modules.theme.domain.event.ThemeActivatedEvent;
import com.capysoft.tuevento.modules.theme.domain.event.ThemeCustomizationResetEvent;
import com.capysoft.tuevento.modules.theme.domain.event.ThemeCustomizedEvent;
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
@DisplayName("ThemeUpdatedWebSocketListener")
class ThemeUpdatedWebSocketListenerTest {

    private static final String TOPIC = "/topic/users/7/session";
    private static final Map<String, String> PAYLOAD = Map.of("type", "THEME_UPDATED");

    @Mock private SimpMessagingTemplate messagingTemplate;
    @InjectMocks private ThemeUpdatedWebSocketListener listener;

    @Test
    @DisplayName("activación de tema → THEME_UPDATED al canal de sesión del usuario")
    void themeActivated_notifiesUser() {
        listener.onThemeActivated(ThemeActivatedEvent.builder().userId(7).themeId(1).build());

        verify(messagingTemplate).convertAndSend(TOPIC, PAYLOAD);
    }

    @Test
    @DisplayName("personalización de tema → THEME_UPDATED")
    void themeCustomized_notifiesUser() {
        listener.onThemeCustomized(ThemeCustomizedEvent.builder().userId(7).property("primary").build());

        verify(messagingTemplate).convertAndSend(TOPIC, PAYLOAD);
    }

    @Test
    @DisplayName("reseteo de personalización → THEME_UPDATED")
    void customizationReset_notifiesUser() {
        listener.onCustomizationReset(ThemeCustomizationResetEvent.builder().userId(7).build());

        verify(messagingTemplate).convertAndSend(TOPIC, PAYLOAD);
    }

    @Test
    @DisplayName("sin userId → no envía nada")
    void nullUser_doesNothing() {
        listener.onThemeActivated(ThemeActivatedEvent.builder().build());

        verifyNoInteractions(messagingTemplate);
    }

    @Test
    @DisplayName("fallo del broker → no lanza excepción")
    void brokerFailure_isNonBreaking() {
        doThrow(new RuntimeException("Broker down"))
                .when(messagingTemplate).convertAndSend(anyString(), any(Object.class));

        assertThatNoException().isThrownBy(() ->
                listener.onThemeActivated(ThemeActivatedEvent.builder().userId(7).build()));
    }
}
