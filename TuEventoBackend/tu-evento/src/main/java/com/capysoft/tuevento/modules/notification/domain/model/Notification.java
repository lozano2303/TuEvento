package com.capysoft.tuevento.modules.notification.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    private Long notificationId;
    private Long channelId;
    private Long notificationTypeId;
    private String entityType;
    private Long entityId;
    private String subject;
    private String body;
    private LocalDateTime sentAt;
    private String idempotencyKey;
}
