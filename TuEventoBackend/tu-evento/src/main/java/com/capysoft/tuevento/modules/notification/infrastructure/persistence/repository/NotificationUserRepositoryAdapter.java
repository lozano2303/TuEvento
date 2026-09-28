package com.capysoft.tuevento.modules.notification.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.notification.application.dto.InAppNotificationDetails;
import com.capysoft.tuevento.modules.notification.domain.model.NotificationUser;
import com.capysoft.tuevento.modules.notification.domain.repository.NotificationUserRepository;
import com.capysoft.tuevento.modules.notification.infrastructure.persistence.entity.NotificationEntity;
import com.capysoft.tuevento.modules.notification.infrastructure.persistence.entity.NotificationUserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class NotificationUserRepositoryAdapter implements NotificationUserRepository {

    private final JpaNotificationUserRepository jpaNotificationUserRepository;
    private final JpaNotificationRepository jpaNotificationRepository;

    @Override
    public NotificationUser save(NotificationUser notificationUser) {
        NotificationUserEntity entity = toEntity(notificationUser);
        NotificationUserEntity saved = jpaNotificationUserRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Page<NotificationUser> findInAppByUserId(Integer userId, boolean unreadOnly, Pageable pageable) {
        return jpaNotificationUserRepository.findInAppNotificationsByUserId(userId, unreadOnly, pageable)
                .map(this::toDomain);
    }

    public Page<InAppNotificationDetails> findInAppDetailsWithNotificationByUserId(Integer userId, boolean unreadOnly, Pageable pageable) {
        return jpaNotificationUserRepository.findInAppNotificationsByUserId(userId, unreadOnly, pageable)
                .map(this::toDetailsWithNotification);
    }

    @Override
    public long countUnreadInAppByUserId(Integer userId) {
        return jpaNotificationUserRepository.countUnreadByUserId(userId);
    }

    @Override
    public Optional<NotificationUser> findByIdAndUserId(Long notificationUserId, Integer userId) {
        return jpaNotificationUserRepository.findById(notificationUserId)
                .filter(entity -> entity.getUserId().equals(userId))
                .map(this::toDomain);
    }

    @Override
    public int markAllInAppRead(Integer userId, LocalDateTime readAt) {
        return jpaNotificationUserRepository.markAllAsReadByUserId(userId);
    }

    // Métodos adicionales para uso interno
    public Optional<NotificationUser> findByNotificationIdAndUserId(Long notificationId, Integer userId) {
        return jpaNotificationUserRepository.findByNotificationIdAndUserId(notificationId, userId)
                .map(this::toDomain);
    }

    private NotificationUser toDomain(NotificationUserEntity entity) {
        return NotificationUser.builder()
                .notificationUserId(entity.getNotificationUserId())
                .notificationId(entity.getNotification().getNotificationId())
                .userId(entity.getUserId())
                .emailAddress(entity.getEmailAddress())
                .deliveredStatus(entity.getDeliveredStatus())
                .errorMessage(entity.getErrorMessage())
                .readAt(entity.getReadAt())
                .build();
    }

    private NotificationUserEntity toEntity(NotificationUser domain) {
        // Para crear/actualizar, necesitamos obtener la notificación
        NotificationEntity notification = null;
        if (domain.getNotificationId() != null) {
            notification = jpaNotificationRepository.findById(domain.getNotificationId())
                    .orElseThrow(() -> new IllegalStateException("Notification not found: " + domain.getNotificationId()));
        }

        return NotificationUserEntity.builder()
                .notificationUserId(domain.getNotificationUserId())
                .notification(notification)
                .userId(domain.getUserId())
                .emailAddress(domain.getEmailAddress())
                .deliveredStatus(domain.getDeliveredStatus())
                .errorMessage(domain.getErrorMessage())
                .readAt(domain.getReadAt())
                .build();
    }

    private InAppNotificationDetails toDetailsWithNotification(NotificationUserEntity entity) {
        return InAppNotificationDetails.builder()
                .notificationUserId(entity.getNotificationUserId())
                .userId(entity.getUserId())
                .emailAddress(entity.getEmailAddress())
                .deliveredStatus(entity.getDeliveredStatus())
                .errorMessage(entity.getErrorMessage())
                .readAt(entity.getReadAt())
                .notificationId(entity.getNotification().getNotificationId())
                .entityType(entity.getNotification().getEntityType())
                .entityId(entity.getNotification().getEntityId())
                .subject(entity.getNotification().getSubject())
                .body(entity.getNotification().getBody())
                .sentAt(entity.getNotification().getSentAt())
                .idempotencyKey(entity.getNotification().getIdempotencyKey())
                .typeName(entity.getNotification().getNotificationType().getName())
                .channelName(entity.getNotification().getChannel().getName())
                .build();
    }
}