package com.capysoft.tuevento.modules.language.domain.repository;

import com.capysoft.tuevento.modules.language.domain.model.NotificationTranslation;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de dominio para NotificationTranslation.
 */
public interface NotificationTranslationRepository {

    /**
     * Busca una traducción específica por notificación y idioma.
     */
    Optional<NotificationTranslation> findByNotificationAndLanguage(Long notificationId, Integer languageId);

    /**
     * Busca todas las traducciones de una notificación.
     */
    List<NotificationTranslation> findByNotification(Long notificationId);

    /**
     * Busca todas las traducciones en un idioma específico.
     */
    List<NotificationTranslation> findByLanguage(Integer languageId);

    /**
     * Busca traducciones por estado.
     */
    List<NotificationTranslation> findByStatus(TranslationStatus status);

    /**
     * Busca traducciones por notificación y estado.
     */
    List<NotificationTranslation> findByNotificationAndStatus(Long notificationId, TranslationStatus status);

    /**
     * Guarda una traducción.
     */
    NotificationTranslation save(NotificationTranslation translation);

    /**
     * Guarda múltiples traducciones.
     */
    List<NotificationTranslation> saveAll(List<NotificationTranslation> translations);

    /**
     * Elimina una traducción.
     */
    void delete(NotificationTranslation translation);

    /**
     * Elimina todas las traducciones de una notificación.
     */
    void deleteByNotification(Long notificationId);

    /**
     * Verifica si existe una traducción para notificación y idioma específicos.
     */
    boolean existsByNotificationAndLanguage(Long notificationId, Integer languageId);
}