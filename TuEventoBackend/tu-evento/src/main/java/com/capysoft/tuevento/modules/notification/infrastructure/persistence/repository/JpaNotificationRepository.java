package com.capysoft.tuevento.modules.notification.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.notification.infrastructure.persistence.entity.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaNotificationRepository extends JpaRepository<NotificationEntity, Long> {
    
    boolean existsByIdempotencyKey(String idempotencyKey);
}