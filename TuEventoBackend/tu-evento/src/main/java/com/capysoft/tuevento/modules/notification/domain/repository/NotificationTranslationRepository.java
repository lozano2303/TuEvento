package com.capysoft.tuevento.modules.notification.domain.repository;

import com.capysoft.tuevento.modules.notification.domain.model.NotificationTranslation;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de dominio para NotificationTranslation.
 */
public interface NotificationTranslationRepository {

    Optional<NotificationTranslation> findByNotificationAndLanguage(Integer notificationId, Integer languageId);

    /** Todas las traducciones de una notificación, en cualquier idioma. */
    List<NotificationTranslation> findAllByNotificationId(Integer notificationId);

    NotificationTranslation save(NotificationTranslation translation);
}
