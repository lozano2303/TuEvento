package com.capysoft.tuevento.modules.language.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.language.infrastructure.persistence.entity.NotificationTypeTranslationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationTypeTranslationJpaRepository extends JpaRepository<NotificationTypeTranslationEntity, Integer> {
    
    List<NotificationTypeTranslationEntity> findByNotificationTypeId(Integer notificationTypeId);
    
    Optional<NotificationTypeTranslationEntity> findByNotificationTypeIdAndLanguageId(Integer notificationTypeId, Integer languageId);
    
    List<NotificationTypeTranslationEntity> findByLanguageId(Integer languageId);
}