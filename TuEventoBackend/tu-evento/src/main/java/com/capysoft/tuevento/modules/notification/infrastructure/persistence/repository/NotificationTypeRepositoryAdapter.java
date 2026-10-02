package com.capysoft.tuevento.modules.notification.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.notification.domain.model.NotificationType;
import com.capysoft.tuevento.modules.notification.domain.repository.NotificationTypeRepository;
import com.capysoft.tuevento.modules.notification.infrastructure.persistence.entity.NotificationTypeEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class NotificationTypeRepositoryAdapter implements NotificationTypeRepository {

    private final JpaNotificationTypeRepository jpaNotificationTypeRepository;

    @Override
    public Optional<NotificationType> findByName(String name) {
        return jpaNotificationTypeRepository.findByName(name)
                .map(this::toDomain);
    }

    @Override
    public NotificationType save(NotificationType notificationType) {
        NotificationTypeEntity entity = toEntity(notificationType);
        NotificationTypeEntity saved = jpaNotificationTypeRepository.save(entity);
        return toDomain(saved);
    }

    private NotificationType toDomain(NotificationTypeEntity entity) {
        return NotificationType.builder()
                .notificationTypeId(entity.getNotificationTypeId())
                .name(entity.getName())
                .description(entity.getDescription())
                .active(entity.getActive())
                .build();
    }

    private NotificationTypeEntity toEntity(NotificationType domain) {
        return NotificationTypeEntity.builder()
                .notificationTypeId(domain.getNotificationTypeId())
                .name(domain.getName())
                .description(domain.getDescription())
                .active(domain.isActive())
                .build();
    }
}