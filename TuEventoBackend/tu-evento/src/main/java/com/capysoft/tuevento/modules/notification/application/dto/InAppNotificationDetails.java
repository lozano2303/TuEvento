package com.capysoft.tuevento.modules.notification.application.dto;

import com.capysoft.tuevento.modules.notification.domain.model.DeliveredStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO que incluye toda la información de la notificación para mapear correctamente.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InAppNotificationDetails {
    // NotificationUser
    private Long notificationUserId;
    private Integer userId;
    private String emailAddress;
    private DeliveredStatus deliveredStatus;
    private String errorMessage;
    private LocalDateTime readAt;
    
    // Notification
    private Long notificationId;
    private String entityType;
    private Long entityId;
    private String subject;
    private String body;
    private LocalDateTime sentAt;
    private String idempotencyKey;
    
    // NotificationType
    private String typeName;
    
    // Channel
    private String channelName;
}