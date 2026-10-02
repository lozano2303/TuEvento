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
}
