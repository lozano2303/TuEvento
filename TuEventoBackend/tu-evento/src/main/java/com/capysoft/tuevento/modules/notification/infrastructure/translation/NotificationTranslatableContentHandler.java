package com.capysoft.tuevento.modules.notification.infrastructure.translation;

import com.capysoft.tuevento.modules.language.application.port.out.TranslatableContentHandler;
import com.capysoft.tuevento.modules.notification.domain.model.NotificationTranslation;
import com.capysoft.tuevento.modules.language.domain.repository.LanguageRepository;
import com.capysoft.tuevento.modules.notification.domain.repository.NotificationTranslationRepository;
import com.capysoft.tuevento.modules.notification.infrastructure.persistence.repository.JpaNotificationRepository;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationSource;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Handler para traducir contenido de notificaciones.
 * Implementa la interfaz para el sistema de traducción usando tabla notification_translation.
 */
@Component
@Slf4j
public class NotificationTranslatableContentHandler implements TranslatableContentHandler {

    private final JpaNotificationRepository jpaRepository;
    private final NotificationTranslationRepository notificationTranslationRepository;
    private final LanguageRepository languageRepository;

    @java.beans.ConstructorProperties({"jpaRepository", "notificationTranslationRepository", "languageRepository"})
    @java.lang.SuppressWarnings("all")
    public NotificationTranslatableContentHandler(final JpaNotificationRepository jpaRepository, final NotificationTranslationRepository notificationTranslationRepository, final LanguageRepository languageRepository) {
        this.jpaRepository = jpaRepository;
        this.notificationTranslationRepository = notificationTranslationRepository;
        this.languageRepository = languageRepository;
    }

    @Override
    public String entityType() {
        return "notification";
    }

    @Override
    public Map<String, String> loadSourceTexts(Long entityId) {
        log.debug("Loading source texts for notification {}", entityId);
        
        // Buscar la notificación en la base de datos usando el repositorio JPA directamente
        var notificationEntityOpt = jpaRepository.findById(entityId);
        
        if (notificationEntityOpt.isEmpty()) {
            log.warn("Notification {} not found for translation", entityId);
            return Map.of();
        }
        
        var notificationEntity = notificationEntityOpt.get();
        
        Map<String, String> texts = new HashMap<>();
        
        if (notificationEntity.getSubject() != null && !notificationEntity.getSubject().trim().isEmpty()) {
            texts.put("subject", notificationEntity.getSubject());
        }
        
        if (notificationEntity.getBody() != null && !notificationEntity.getBody().trim().isEmpty()) {
            texts.put("body", notificationEntity.getBody());
        }
        
        log.debug("Loaded {} source texts for notification {}", texts.size(), entityId);
        return texts;
    }

    @Override
    public void saveTranslation(Long entityId, String languageCode, Map<String, String> translatedTexts, 
                               TranslationSource source, TranslationStatus status) {
        log.info("=== SAVETRANSLATION START === Notification: {}, Lang: {}, Texts: {}", entityId, languageCode, translatedTexts);
        
        // 1. Buscar el idioma por código
        var language = languageRepository.findByCode(languageCode);
        if (language.isEmpty()) {
            log.error("CRITICAL: Language {} not found for saving translation", languageCode);
            return;
        }
        
        Integer languageId = language.get().getLanguageId();
        log.info("Language found: {} -> ID: {}", languageCode, languageId);
        
        // 2. Buscar traducción existente o crear nueva
        Optional<NotificationTranslation> existingTranslation = 
                notificationTranslationRepository.findByNotificationAndLanguage(entityId.intValue(), languageId);
        
        NotificationTranslation translation;
        if (existingTranslation.isPresent()) {
            // Actualizar traducción existente
            translation = existingTranslation.get();
            log.info("Updating existing translation {} for notification {}/{}", 
                     translation.getTranslationId(), entityId, languageCode);
        } else {
            // Crear nueva traducción
            translation = NotificationTranslation.builder()
                    .notificationId(entityId.intValue())
                    .languageId(languageId)
                    .source(source)
                    .status(status)
                    .build();
            log.info("Creating NEW translation for notification {}/{}", entityId, languageCode);
        }
        
        // 3. Actualizar contenido traducido
        boolean updated = false;
        
        if (translatedTexts.containsKey("subject")) {
            String translatedSubject = translatedTexts.get("subject");
            log.info("Processing subject: '{}'", translatedSubject);
            if (translatedSubject != null && !translatedSubject.trim().isEmpty()) {
                translation.setTranslatedSubject(translatedSubject);
                log.info("Set translatedSubject: '{}'", translatedSubject);
                updated = true;
            }
        }
        
        if (translatedTexts.containsKey("body")) {
            String translatedBody = translatedTexts.get("body");
            log.info("Processing body: '{}'", translatedBody);
            if (translatedBody != null && !translatedBody.trim().isEmpty()) {
                translation.setTranslatedBody(translatedBody);
                log.info("Set translatedBody: '{}'", translatedBody);
                updated = true;
            }
        }
        
        log.info("Updated flag: {}, Source: {}, Status: {}", updated, source, status);
        
        if (updated) {
            translation.setSource(source);
            translation.setStatus(status);
            
            log.info("CALLING notificationTranslationRepository.save()...");
            NotificationTranslation savedTranslation = notificationTranslationRepository.save(translation);
            log.info("✅ SAVED SUCCESSFULLY - Translation ID: {} for notification {} in language {}", 
                    savedTranslation.getTranslationId(), entityId, languageCode);
        } else {
            log.warn("❌ NO UPDATES - No translations applied to notification {}", entityId);
        }
        
        log.info("=== SAVETRANSLATION END ===");
    }

    @Override
    public Optional<TranslationStatus> findTranslationStatus(Long entityId, String languageCode) {
        log.debug("Finding translation status for notification {} in language {}", entityId, languageCode);
        
        var language = languageRepository.findByCode(languageCode);
        if (language.isEmpty()) {
            return Optional.empty();
        }
        
        return notificationTranslationRepository
                .findByNotificationAndLanguage(entityId.intValue(), language.get().getLanguageId())
                .map(NotificationTranslation::getStatus);
    }
}
