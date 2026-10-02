package com.capysoft.tuevento.modules.notification.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.notification.domain.model.Notification;
import com.capysoft.tuevento.modules.notification.domain.repository.NotificationRepository;
import com.capysoft.tuevento.modules.notification.infrastructure.persistence.entity.ChannelEntity;
import com.capysoft.tuevento.modules.notification.infrastructure.persistence.entity.NotificationEntity;
import com.capysoft.tuevento.modules.notification.infrastructure.persistence.entity.NotificationTypeEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class NotificationRepositoryAdapter implements NotificationRepository {

    private final JpaNotificationRepository jpaNotificationRepository;
    private final JpaChannelRepository jpaChannelRepository;
    private final JpaNotificationTypeRepository jpaNotificationTypeRepository;

    @Override
    public boolean existsByIdempotencyKey(String idempotencyKey) {
        return jpaNotificationRepository.existsByIdempotencyKey(idempotencyKey);
    }

    @Override
    public Notification save(Notification notification) {
        NotificationEntity entity = toEntity(notification);
        NotificationEntity saved = jpaNotificationRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Notification> findById(Long id) {
        return jpaNotificationRepository.findById(id)
                .map(this::toDomain);
    }

    private Notification toDomain(NotificationEntity entity) {
        return Notification.builder()
                .notificationId(entity.getNotificationId())
                .channelId(entity.getChannel().getChannelId())
                .notificationTypeId(entity.getNotificationType().getNotificationTypeId())
                .entityType(entity.getEntityType())
                .entityId(entity.getEntityId())
                .subject(entity.getSubject())
                .body(entity.getBody())
                .sentAt(entity.getSentAt())
                .idempotencyKey(entity.getIdempotencyKey())
                .build();
    }

    private NotificationEntity toEntity(Notification domain) {
        // Para crear una nueva notificación, necesitamos obtener las entidades relacionadas
        ChannelEntity channel = jpaChannelRepository.findById(domain.getChannelId())
                .orElseThrow(() -> new IllegalStateException("Channel not found: " + domain.getChannelId()));
        
        NotificationTypeEntity notificationType = jpaNotificationTypeRepository.findById(domain.getNotificationTypeId())
                .orElseThrow(() -> new IllegalStateException("NotificationType not found: " + domain.getNotificationTypeId()));

        return NotificationEntity.builder()
                .notificationId(domain.getNotificationId())
                .channel(channel)
                .notificationType(notificationType)
                .entityType(domain.getEntityType())
                .entityId(domain.getEntityId())
                .subject(domain.getSubject())
                .body(domain.getBody())
                .sentAt(domain.getSentAt())
                .idempotencyKey(domain.getIdempotencyKey())
                .build();
    }
}