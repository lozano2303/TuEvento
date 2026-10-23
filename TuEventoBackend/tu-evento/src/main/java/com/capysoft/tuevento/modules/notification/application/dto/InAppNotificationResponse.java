package com.capysoft.tuevento.modules.notification.application.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
    private LocalDateTime readAt;
    private boolean read;
    private List<NotificationTranslationDTO> translations;
}
