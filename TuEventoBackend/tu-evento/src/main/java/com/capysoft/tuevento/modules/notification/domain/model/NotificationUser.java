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
public class NotificationUser {
    private Long notificationUserId;
    private Long notificationId;
    private Integer userId;
    private String emailAddress;
    private DeliveredStatus deliveredStatus;
    private String errorMessage;
    private LocalDateTime readAt;
}
