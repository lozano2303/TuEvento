package com.capysoft.tuevento.modules.catalogbridge.messaging;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

@DisplayName("CommercePushRelay")
class CommercePushRelayTest {

    private final SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
    private final CommercePushRelay relay = new CommercePushRelay(template, new ObjectMapper());

    @Test
    @DisplayName("reenvía el aviso al canal de entradas del usuario")
    void relays() {
        relay.relay("{\"destination\":\"/topic/users/7/tickets\",\"body\":{\"type\":\"QR_VALIDATED\"}}");

        verify(template).convertAndSend(eq("/topic/users/7/tickets"), (Object) eq(Map.of("type", "QR_VALIDATED")));
    }

    @Test
    @DisplayName("descarta cualquier otro destino y los mensajes ilegibles")
    void discards() {
        relay.relay("{\"destination\":\"/topic/seats/1\",\"body\":{}}");
        relay.relay("{\"body\":{}}");

        verifyNoInteractions(template);
        assertThatCode(() -> relay.relay("not json")).doesNotThrowAnyException();
    }
}
