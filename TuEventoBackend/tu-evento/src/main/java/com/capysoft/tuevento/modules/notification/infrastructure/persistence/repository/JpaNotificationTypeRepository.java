package com.capysoft.tuevento.modules.notification.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.notification.infrastructure.persistence.entity.NotificationTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaNotificationTypeRepository extends JpaRepository<NotificationTypeEntity, Long> {
    
    Optional<NotificationTypeEntity> findByName(String name);
}