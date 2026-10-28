package com.capysoft.notification.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import com.capysoft.notification.application.dto.NotificationRecipient;
import com.capysoft.notification.application.dto.SendNotificationCommand;
import com.capysoft.notification.application.usecase.SendNotificationUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@DisplayName("Mensajería Kafka del servicio de notificaciones")
class NotificationMessagingTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Nested
    @DisplayName("consumo de comandos del monolito")
    class Commands {

        private final SendNotificationUseCase useCase = mock(SendNotificationUseCase.class);
        private final NotificationCommandListener listener = new NotificationCommandListener(useCase, objectMapper);

        @Test
        @DisplayName("convierte el mensaje en el mismo comando que enviaba el monolito, con los destinatarios")
        void mapsMessageToCommand() {
            listener.onCommand("""
                    {"messageId":"m-1","occurredAt":"2026-10-26T15:04:05Z",
                     "command":{"typeName":"PAYMENT_APPROVED","entityType":"PAYMENT","entityId":9,"userIds":[7],
                                "gatewayAmount":15000.50,"currency":"COP","idempotencySuffix":"s1","eventName":"Concierto"},
                     "recipients":[{"userId":7,"email":"ana@test.com","preferredLanguageId":2}]}
                    """);

            ArgumentCaptor<SendNotificationCommand> command = ArgumentCaptor.forClass(SendNotificationCommand.class);
            verify(useCase).execute(command.capture());
            SendNotificationCommand c = command.getValue();
            assertThat(c.getTypeName()).isEqualTo("PAYMENT_APPROVED");
            assertThat(c.getEntityId()).isEqualTo(9L);
            assertThat(c.getUserIds()).containsExactly(7);
            assertThat(c.getGatewayAmount()).isEqualByComparingTo(new BigDecimal("15000.50"));
            assertThat(c.getIdempotencySuffix()).isEqualTo("s1");
            assertThat(c.getEventName()).isEqualTo("Concierto");
            assertThat(c.getRecipients()).containsExactly(new NotificationRecipient(7, "ana@test.com", 2L));
        }

        @Test
        @DisplayName("un mensaje ilegible o sin comando se descarta sin enviar nada")
        void discardsInvalidMessages() {
            listener.onCommand("no es json");
            listener.onCommand("{\"messageId\":\"m-2\"}");

            verifyNoInteractions(useCase);
        }
    }

    @Nested
    @DisplayName("publicación hacia el monolito")
    @SuppressWarnings("unchecked")
    class Events {

        private final KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
        private final KafkaNotificationEventsPublisher publisher = new KafkaNotificationEventsPublisher(kafka, objectMapper);

        @Test
        @DisplayName("el push va al topic de notificaciones del usuario, con el mismo cuerpo que el WebSocket")
        void publishesPush() throws Exception {
            publisher.push(7, Map.of("subject", "Pago aprobado"));

            ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
            verify(kafka).send(eq(NotificationTopics.PUSH), eq("7"), json.capture());
            Map<String, Object> message = objectMapper.readValue(json.getValue(), Map.class);
            assertThat(message).containsEntry("destination", "/topic/notifications/7")
                    .containsEntry("body", Map.of("subject", "Pago aprobado"));
        }

        @Test
        @DisplayName("la petición de traducción lleva el id de la notificación")
        void publishesTranslationRequest() {
            publisher.requestTranslation(42L);

            verify(kafka).send(NotificationTopics.TRANSLATION, "42", "{\"notificationId\":42}");
        }

        @Test
        @DisplayName("un fallo de Kafka no se propaga")
        void kafkaFailure_isSwallowed() {
            when(kafka.send(anyString(), anyString(), anyString())).thenThrow(new IllegalStateException("broker down"));

            publisher.requestTranslation(42L);
            publisher.push(7, Map.of());
        }
    }
}
