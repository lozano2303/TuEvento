package com.capysoft.notification.infrastructure.messaging;

import java.util.List;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.capysoft.notification.application.dto.NotificationRecipient;
import com.capysoft.notification.application.dto.SendNotificationCommand;
import com.capysoft.notification.application.usecase.SendNotificationUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Consume las notificaciones que publica el monolito y las envía. Los reintentos de Kafka son
 * seguros: {@link SendNotificationUseCase} descarta las repetidas por su idempotencyKey. Un
 * mensaje que no se puede leer se registra y se descarta, sin bloquear la partición.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationCommandListener {

    private final SendNotificationUseCase sendNotificationUseCase;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = NotificationTopics.COMMANDS, groupId = "notificaciones")
    public void onCommand(String rawMessage) {
        NotificationCommandMessage message;
        try {
            message = objectMapper.readValue(rawMessage, NotificationCommandMessage.class);
        } catch (Exception e) {
            log.error("Discarding unreadable notification message", e);
            return;
        }
        if (message.command() == null) {
            log.warn("Discarding notification message without command: messageId={}", message.messageId());
            return;
        }
        sendNotificationUseCase.execute(toCommand(message));
    }

    static SendNotificationCommand toCommand(NotificationCommandMessage message) {
        NotificationCommandMessage.Command c = message.command();
        List<NotificationRecipient> recipients = message.recipients() == null ? null
                : message.recipients().stream()
                        .map(r -> new NotificationRecipient(r.userId(), r.email(), r.preferredLanguageId()))
                        .toList();
        return SendNotificationCommand.builder()
                .typeName(c.typeName()).entityType(c.entityType()).entityId(c.entityId())
                .userIds(c.userIds()).recipients(recipients)
                .gatewayAmount(c.gatewayAmount()).walletAmount(c.walletAmount()).creditedAmount(c.creditedAmount())
                .currency(c.currency()).reason(c.reason()).idempotencySuffix(c.idempotencySuffix())
                .eventName(c.eventName()).commenterName(c.commenterName())
                .build();
    }
}
