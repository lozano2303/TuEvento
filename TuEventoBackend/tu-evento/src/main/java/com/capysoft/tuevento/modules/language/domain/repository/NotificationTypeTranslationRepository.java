package com.capysoft.tuevento.modules.language.domain.repository;

import com.capysoft.tuevento.modules.language.domain.model.NotificationTypeTranslation;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de dominio para NotificationTypeTranslation.
 */
public interface NotificationTypeTranslationRepository {

    /**
     * Busca una traducción específica por notification type y idioma.
     */
    Optional<NotificationTypeTranslation> findByNotificationTypeAndLanguage(Long notificationTypeId, Integer languageId);

    /**
     * Busca todas las traducciones de un notification type.
     */
    List<NotificationTypeTranslation> findByNotificationType(Long notificationTypeId);

    /**
     * Busca todas las traducciones en un idioma específico.
     */
    List<NotificationTypeTranslation> findByLanguage(Integer languageId);

    /**
     * Busca traducciones por estado.
     */
    List<NotificationTypeTranslation> findByStatus(TranslationStatus status);

    /**
     * Busca traducciones por notification type y estado.
     */
    List<NotificationTypeTranslation> findByNotificationTypeAndStatus(Long notificationTypeId, TranslationStatus status);

    /**
     * Guarda una traducción.
     */
    NotificationTypeTranslation save(NotificationTypeTranslation translation);

    /**
     * Guarda múltiples traducciones.
     */
    List<NotificationTypeTranslation> saveAll(List<NotificationTypeTranslation> translations);

    /**
     * Elimina una traducción.
     */
    void delete(NotificationTypeTranslation translation);

    /**
     * Elimina todas las traducciones de un notification type.
     */
    void deleteByNotificationType(Long notificationTypeId);

    /**
     * Verifica si existe una traducción para notification type y idioma específicos.
     */
    boolean existsByNotificationTypeAndLanguage(Long notificationTypeId, Integer languageId);
}