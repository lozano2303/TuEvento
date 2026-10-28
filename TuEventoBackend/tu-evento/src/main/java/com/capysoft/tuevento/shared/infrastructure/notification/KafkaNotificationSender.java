package com.capysoft.tuevento.shared.infrastructure.notification;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.capysoft.tuevento.shared.application.notification.SendNotificationCommand;
import com.capysoft.tuevento.shared.application.notification.NotificationSender;
import com.capysoft.tuevento.shared.application.notification.RecipientDirectoryPort;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Publica la notificación en {@link NotificationTopics#COMMANDS} para que la envíe
 * notification-service. Los listeners que llaman aquí corren tras el commit, así que solo se
 * publica lo confirmado. Un fallo de Kafka se registra y no rompe el flujo que lo originó: se
 * pierde ese aviso.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaNotificationSender implements NotificationSender {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final RecipientDirectoryPort recipientDirectoryPort;

    @Override
    public void send(SendNotificationCommand command) {
        if (command == null || command.getUserIds() == null || command.getUserIds().isEmpty()) {
            log.warn("Notification not published: no recipients, type={}", command == null ? null : command.getTypeName());
            return;
        }
        try {
            NotificationCommandMessage message = new NotificationCommandMessage(
                    UUID.randomUUID().toString(), Instant.now(), toMessage(command), recipients(command.getUserIds()));
            // Clave = primer destinatario: sus avisos van a la misma partición y conservan el orden
            kafkaTemplate.send(NotificationTopics.COMMANDS, String.valueOf(command.getUserIds().get(0)),
                    objectMapper.writeValueAsString(message));
        } catch (Exception e) {
            log.error("Could not publish notification type={} entityId={}", command.getTypeName(), command.getEntityId(), e);
        }
    }

    /** Email e idioma de cada destinatario, en una sola consulta; uno desconocido va sin ellos. */
    private List<NotificationCommandMessage.Recipient> recipients(List<Integer> userIds) {
        Map<Integer, RecipientDirectoryPort.Recipient> found = recipientDirectoryPort.findRecipients(userIds);
        return userIds.stream()
                .map(userId -> {
                    RecipientDirectoryPort.Recipient r = found.get(userId);
                    return new NotificationCommandMessage.Recipient(userId,
                            r == null ? null : r.email(),
                            r == null ? null : r.preferredLanguageId());
                })
                .toList();
    }

    private static NotificationCommandMessage.Command toMessage(SendNotificationCommand c) {
        return new NotificationCommandMessage.Command(c.getTypeName(), c.getEntityType(), c.getEntityId(),
                c.getUserIds(), c.getGatewayAmount(), c.getWalletAmount(), c.getCreditedAmount(), c.getCurrency(),
                c.getReason(), c.getIdempotencySuffix(), c.getEventName(), c.getCommenterName());
    }
}
