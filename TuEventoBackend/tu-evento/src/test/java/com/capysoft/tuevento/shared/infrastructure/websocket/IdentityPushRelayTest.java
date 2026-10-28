package com.capysoft.tuevento.shared.infrastructure.websocket;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

@DisplayName("IdentityPushRelay (identity-service → /ws)")
class IdentityPushRelayTest {

    private final SimpMessagingTemplate messaging = mock(SimpMessagingTemplate.class);
    private final IdentityPushRelay relay = new IdentityPushRelay(messaging, new ObjectMapper());

    @Test
    @DisplayName("THEME_UPDATED al canal de sesión de un usuario → se reenvía con el payload de siempre")
    void relaysThemeUpdated() {
        relay.relay("{\"destination\":\"/topic/users/7/session\",\"body\":{\"type\":\"THEME_UPDATED\",\"extra\":1}}");

        verify(messaging).convertAndSend("/topic/users/7/session", Map.of("type", "THEME_UPDATED"));
    }

    @Test
    @DisplayName("LANGUAGE_UPDATED se reenvía con su languageId; sin languageId se descarta")
    void relaysLanguageUpdated() {
        relay.relay("{\"destination\":\"/topic/users/7/session\",\"body\":{\"type\":\"LANGUAGE_UPDATED\",\"languageId\":2}}");
        relay.relay("{\"destination\":\"/topic/users/7/session\",\"body\":{\"type\":\"LANGUAGE_UPDATED\"}}");

        verify(messaging).convertAndSend("/topic/users/7/session", Map.of("type", "LANGUAGE_UPDATED", "languageId", 2L));
        org.mockito.Mockito.verifyNoMoreInteractions(messaging);
    }

    @Test
    @DisplayName("otros canales o tipos que cierran la sesión (ROLE_REVOKED) se descartan")
    void discardsAnythingElse() {
        relay.relay("{\"destination\":\"/topic/users/7/session\",\"body\":{\"type\":\"ROLE_REVOKED\"}}");
        relay.relay("{\"destination\":\"/topic/events/3/seats\",\"body\":{\"type\":\"THEME_UPDATED\"}}");
        relay.relay("{\"destination\":\"/topic/users/*/session\",\"body\":{\"type\":\"THEME_UPDATED\"}}");
        relay.relay("{\"destination\":\"/topic/users/7/session\"}");

        verifyNoInteractions(messaging);
    }

    @Test
    @DisplayName("un mensaje mal formado no rompe el consumidor")
    void malformedIsIgnored() {
        assertThatNoException().isThrownBy(() -> relay.relay("not json"));
        verifyNoInteractions(messaging);
    }
}
