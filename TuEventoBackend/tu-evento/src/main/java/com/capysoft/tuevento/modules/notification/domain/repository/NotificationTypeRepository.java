package com.capysoft.tuevento.modules.notification.domain.repository;

import com.capysoft.tuevento.modules.notification.domain.model.NotificationType;

import java.util.Optional;

public interface NotificationTypeRepository {
    Optional<NotificationType> findByName(String name);
    NotificationType save(NotificationType notificationType);
}
