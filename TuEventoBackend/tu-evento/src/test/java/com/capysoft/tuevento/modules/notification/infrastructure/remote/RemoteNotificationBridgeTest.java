package com.capysoft.tuevento.modules.notification.infrastructure.remote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.capysoft.tuevento.shared.application.translation.TranslationRequests;
import com.capysoft.tuevento.modules.notification.application.dto.SendNotificationCommand;
import com.capysoft.tuevento.modules.notification.application.usecase.SendNotificationUseCase;
import com.capysoft.tuevento.modules.notification.application.port.out.RecipientDirectoryPort;
import com.capysoft.tuevento.modules.notification.application.port.out.RecipientDirectoryPort.Recipient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@DisplayName("Puente del monolito con el microservicio de notificaciones")
class RemoteNotificationBridgeTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private static SendNotificationCommand command(List<Integer> userIds) {
        return SendNotificationCommand.builder().typeName("PAYMENT_APPROVED").entityType("PAYMENT").entityId(9L)
                .userIds(userIds).gatewayAmount(new BigDecimal("15000.50")).currency("COP").build();
    }

    @Nested
    @DisplayName("modo local")
    class Local {

        @Test
        @DisplayName("envía la notificación dentro del monolito")
        void delegatesToUseCase() {
            SendNotificationUseCase useCase = mock(SendNotificationUseCase.class);
            SendNotificationCommand command = command(List.of(7));

            new LocalNotificationSender(useCase).send(command);

            verify(useCase).execute(command);
        }
    }

    @Nested
    @DisplayName("modo remote: publicación en Kafka")
    @SuppressWarnings("unchecked")
    class Publish {

        private final KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
        private final RecipientDirectoryPort directory = mock(RecipientDirectoryPort.class);
        private final KafkaNotificationSender sender =
                new KafkaNotificationSender(kafka, objectMapper, directory);

        @Test
        @DisplayName("publica el comando con el email y el idioma de cada destinatario, con clave = destinatario")
        void publishesCommandWithRecipients() throws Exception {
            // 8 no existe en Identidad: viaja sin email ni idioma
            when(directory.findRecipients(List.of(7, 8)))
                    .thenReturn(Map.of(7, new Recipient(7, "ana", "Ana", "ana@test.com", 2L)));

            sender.send(command(List.of(7, 8)));

            verify(directory).findRecipients(List.of(7, 8));

            ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
            verify(kafka).send(eq(NotificationTopics.COMMANDS), eq("7"), json.capture());
            NotificationCommandMessage message = objectMapper.readValue(json.getValue(), NotificationCommandMessage.class);
            assertThat(message.messageId()).isNotBlank();
            assertThat(message.command().typeName()).isEqualTo("PAYMENT_APPROVED");
            assertThat(message.command().gatewayAmount()).isEqualByComparingTo("15000.50");
            assertThat(message.recipients()).containsExactly(
                    new NotificationCommandMessage.Recipient(7, "ana@test.com", 2L),
                    new NotificationCommandMessage.Recipient(8, null, null));
        }

        @Test
        @DisplayName("sin destinatarios no publica nada")
        void noRecipients_publishesNothing() {
            sender.send(command(List.of()));
            sender.send(null);

            verifyNoInteractions(kafka);
        }

        @Test
        @DisplayName("un fallo de Kafka no rompe el flujo que generó el aviso")
        void kafkaFailure_isSwallowed() {
            when(directory.findRecipients(List.of(7)))
                    .thenReturn(Map.of(7, new Recipient(7, "ana", null, "ana@test.com", null)));
            when(kafka.send(anyString(), anyString(), anyString())).thenThrow(new IllegalStateException("broker down"));

            sender.send(command(List.of(7)));

            verify(kafka).send(anyString(), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("modo remote: relay de pushes al /ws")
    class Relay {

        private final SimpMessagingTemplate messaging = mock(SimpMessagingTemplate.class);
        private final NotificationPushRelay relay = new NotificationPushRelay(messaging, objectMapper);

        @Test
        @DisplayName("reenvía el cuerpo al topic de notificaciones del usuario")
        void relaysToUserTopic() {
            relay.relay("{\"destination\":\"/topic/notifications/7\",\"body\":{\"subject\":\"Pago aprobado\"}}");

            verify(messaging).convertAndSend("/topic/notifications/7", (Object) Map.of("subject", "Pago aprobado"));
        }

        @Test
        @DisplayName("descarta destinos fuera de /topic/notifications/{id} y mensajes inválidos")
        void discardsOtherDestinations() {
            relay.relay("{\"destination\":\"/topic/seats/5\",\"body\":{}}");
            relay.relay("{\"destination\":\"/topic/notifications/7/../../seats\",\"body\":{}}");
            relay.relay("{\"body\":{}}");
            relay.relay("no es json");

            verify(messaging, never()).convertAndSend(anyString(), any(Object.class));
        }
    }

    @Nested
    @DisplayName("modo remote: peticiones de traducción")
    class Translation {

        private final TranslationRequests translationRequests = mock(TranslationRequests.class);
        private final NotificationTranslationRequestListener listener =
                new NotificationTranslationRequestListener(translationRequests, objectMapper);

        @Test
        @DisplayName("encola la traducción de la notificación desde el español")
        void requestsTranslation() {
            listener.onTranslationRequested("{\"notificationId\":42}");

            verify(translationRequests).request("notification", 42L);
        }

        @Test
        @DisplayName("un mensaje sin notificationId o inválido se descarta")
        void discardsInvalidMessages() {
            listener.onTranslationRequested("{}");
            listener.onTranslationRequested("no es json");

            verifyNoInteractions(translationRequests);
        }
    }
}
