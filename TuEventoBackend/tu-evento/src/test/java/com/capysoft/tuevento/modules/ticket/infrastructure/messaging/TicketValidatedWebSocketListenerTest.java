package com.capysoft.tuevento.modules.ticket.infrastructure.messaging;

import com.capysoft.tuevento.modules.ticket.domain.event.TicketsValidatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("TicketValidatedWebSocketListener — aviso de QR validado")
class TicketValidatedWebSocketListenerTest {

    private final SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
    private final TicketValidatedWebSocketListener listener = new TicketValidatedWebSocketListener(template);

    private static final TicketsValidatedEvent EVENT =
            new TicketsValidatedEvent(50L, "ORDER", 300L, 7L, List.of(11L, 12L));

    @Test
    @DisplayName("publica en el canal personal del dueño de los tickets")
    @SuppressWarnings("unchecked")
    void sendsToTheOwnersChannel() {
        listener.onTicketsValidated(EVENT);

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(template).convertAndSend(org.mockito.ArgumentMatchers.eq("/topic/users/50/tickets"), payload.capture());
        Map<String, Object> body = (Map<String, Object>) payload.getValue();
        assertThat(body)
                .containsEntry("type", "QR_VALIDATED")
                .containsEntry("scope", "ORDER")
                .containsEntry("targetId", 300L)
                .containsEntry("eventId", 7L)
                .containsEntry("ticketIds", List.of(11L, 12L));
    }

    @Test
    @DisplayName("un fallo del broker no rompe el escaneo ya guardado")
    void brokerFailureIsSwallowed() {
        doThrow(new MessagingException("broker caído")).when(template).convertAndSend(anyString(), any(Object.class));

        assertThatCode(() -> listener.onTicketsValidated(EVENT)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("la lista de tickets del evento es inmutable")
    void ticketIdsAreCopied() {
        assertThatCode(() -> EVENT.ticketIds().add(1L)).isInstanceOf(UnsupportedOperationException.class);
    }
}
