package com.capysoft.tuevento.modules.notification.application.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class SendNotificationCommand {
    private final String typeName;
    private final String entityType;
    private final Long entityId;
    private final List<Integer> userIds;
    private final BigDecimal gatewayAmount;
    private final BigDecimal walletAmount;
    private final BigDecimal creditedAmount;
    private final String currency;
    private final String reason;
    /**
     * Optional suffix appended to the idempotency key: {@code typeName:entityId:channelName[:suffix]}.
     * Use this for notification types that can fire more than once for the same entity
     * (e.g. EVENT_REJECTED after a re-submission cycle).  Leave null for payment types
     * where one notification per entity is the correct behavior.
     */
    private final String idempotencySuffix;
    /**
     * Human-readable name of the event — used in notification copy for event-related types.
     * Null for non-event notifications.
     */
    private final String eventName;
}
