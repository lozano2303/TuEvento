package com.capysoft.notification.domain.repository;

import com.capysoft.notification.domain.model.NotificationType;

import java.util.Optional;

public interface NotificationTypeRepository {
    Optional<NotificationType> findByName(String name);
    NotificationType save(NotificationType notificationType);
}
