package com.capysoft.tuevento.modules.notification.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.notification.infrastructure.persistence.entity.NotificationTranslationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationTranslationJpaRepository extends JpaRepository<NotificationTranslationEntity, Integer> {

    List<NotificationTranslationEntity> findByNotificationId(Integer notificationId);

    Optional<NotificationTranslationEntity> findByNotificationIdAndLanguageId(Integer notificationId, Integer languageId);
}
