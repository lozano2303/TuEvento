package com.capysoft.notification.application.port.out;

import java.util.List;
import java.util.Optional;

/**
 * Lectura de las traducciones de una notificación, que escribe el sistema de traducciones del
 * monolito en la base de datos compartida. El servicio nunca las modifica.
 */
public interface NotificationTranslationReader {

    /** Traducciones existentes de la notificación, una por idioma. */
    List<TranslationView> findAll(Long notificationId);

    /** Traducción a un idioma concreto, si existe. */
    Optional<TranslationView> find(Long notificationId, Long languageId);

    /** Código ISO del idioma ({@code es}, {@code en}…). */
    Optional<String> languageCode(Long languageId);

    record TranslationView(String languageCode, String subject, String body, String status, String source) {

        public boolean isPublished() {
            return "PUBLISHED".equals(status);
        }
    }
}
