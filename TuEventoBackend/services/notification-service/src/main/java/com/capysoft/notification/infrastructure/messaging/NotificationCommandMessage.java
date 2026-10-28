package com.capysoft.notification.infrastructure.messaging;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Mensaje de {@link NotificationTopics#COMMANDS}, con el mismo formato que publica el
 * {@code KafkaNotificationSender} del monolito.
 */
public record NotificationCommandMessage(
        String messageId,
        Instant occurredAt,
        Command command,
        List<Recipient> recipients) {

    public record Command(
            String typeName,
            String entityType,
            Long entityId,
            List<Integer> userIds,
            BigDecimal gatewayAmount,
            BigDecimal walletAmount,
            BigDecimal creditedAmount,
            String currency,
            String reason,
            String idempotencySuffix,
            String eventName,
            String commenterName) {
    }

    public record Recipient(Integer userId, String email, Long preferredLanguageId) {
    }
}
