package com.capysoft.tuevento.modules.notification.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InAppNotificationResponse {
    private Long notificationUserId;
    private String type;
    private String subject;
    private String body;
    private String entityType;
    private Long entityId;
    private LocalDateTime sentAt;
    private boolean read;
}
