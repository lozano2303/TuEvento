package com.capysoft.tuevento.modules.language.domain.repository;

import com.capysoft.tuevento.modules.language.domain.model.NotificationTypeTranslation;

import java.util.List;
import java.util.Optional;

public interface NotificationTypeTranslationRepository {

    NotificationTypeTranslation save(NotificationTypeTranslation translation);

    Optional<NotificationTypeTranslation> findById(Integer id);

    List<NotificationTypeTranslation> findByNotificationTypeId(Integer notificationTypeId);

    Optional<NotificationTypeTranslation> findByNotificationTypeIdAndLanguageId(Integer notificationTypeId, Integer languageId);

    List<NotificationTypeTranslation> findByLanguageId(Integer languageId);

    void delete(NotificationTypeTranslation translation);

    void deleteById(Integer id);

    boolean existsById(Integer id);

    List<NotificationTypeTranslation> findAll();
}