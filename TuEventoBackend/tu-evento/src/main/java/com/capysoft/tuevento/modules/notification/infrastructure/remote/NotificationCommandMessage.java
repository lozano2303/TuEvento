package com.capysoft.tuevento.modules.notification.infrastructure.remote;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Mensaje de {@link NotificationTopics#COMMANDS}. Lleva el comando completo y los datos de
 * cada destinatario que el servicio necesita para el email, así no lee tablas de Identidad.
 * Mismo formato que {@code NotificationCommandMessage} del microservicio.
 *
 * @param messageId UUID del mensaje; el servicio descarta los repetidos por la idempotencyKey
 */
public record NotificationCommandMessage(
        String messageId,
        Instant occurredAt,
        Command command,
        List<Recipient> recipients) {

    /** Mismos campos que {@code SendNotificationCommand}. */
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

    /** Email e idioma preferido del destinatario, resueltos al publicar. */
    public record Recipient(Integer userId, String email, Long preferredLanguageId) {
    }
}
