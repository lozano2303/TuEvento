package com.capysoft.tuevento.modules.notification.domain.repository;

import com.capysoft.tuevento.modules.notification.domain.model.Notification;

import java.util.Optional;

public interface NotificationRepository {
    Notification save(Notification notification);
    boolean existsByIdempotencyKey(String idempotencyKey);
    Optional<Notification> findById(Long id);
}
