package com.capysoft.commerce.ticket.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import com.capysoft.commerce.ticket.domain.event.TicketsValidatedEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@DisplayName("TicketValidatedPushListener")
class TicketValidatedPushListenerTest {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final TicketValidatedPushListener listener = new TicketValidatedPushListener(kafka, mapper);

    private static TicketsValidatedEvent event() {
        return new TicketsValidatedEvent(7L, "ORDER", 5L, 3L, List.of(1L, 2L));
    }

    @Test
    @DisplayName("publica el destino personal del asistente y el mismo cuerpo que recibía por el /ws")
    void publishesDestinationAndBody() throws Exception {
        listener.onTicketsValidated(event());

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(kafka).send(org.mockito.ArgumentMatchers.eq(TicketValidatedPushListener.PUSH_TOPIC),
                org.mockito.ArgumentMatchers.eq("7"), message.capture());
        JsonNode json = mapper.readTree(message.getValue());
        assertThat(json.get("destination").asText()).isEqualTo("/topic/users/7/tickets");
        assertThat(json.get("body").get("type").asText()).isEqualTo("QR_VALIDATED");
        assertThat(json.get("body").get("targetId").asLong()).isEqualTo(5L);
        assertThat(json.get("body").get("ticketIds")).hasSize(2);
    }

    @Test
    @DisplayName("si Kafka falla no rompe el check-in ya guardado")
    void kafkaFailureIsContained() {
        when(kafka.send(anyString(), anyString(), anyString())).thenThrow(new IllegalStateException("broker down"));

        assertThatCode(() -> listener.onTicketsValidated(event())).doesNotThrowAnyException();
    }
}
