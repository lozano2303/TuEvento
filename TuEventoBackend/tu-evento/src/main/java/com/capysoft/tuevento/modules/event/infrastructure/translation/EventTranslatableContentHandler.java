package com.capysoft.tuevento.modules.event.infrastructure.translation;

import com.capysoft.tuevento.modules.event.domain.model.Event;
import com.capysoft.tuevento.modules.event.domain.repository.EventRepository;
import com.capysoft.tuevento.shared.application.translation.TranslatableContentHandler;
import com.capysoft.tuevento.modules.event.domain.model.EventTranslation;
import com.capysoft.tuevento.modules.event.domain.repository.EventTranslationRepository;
import com.capysoft.tuevento.shared.application.translation.LanguageCatalog;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationSource;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Handler para traducir contenido de eventos.
 * Implementa la interfaz para el sistema de traducción usando tabla event_translation.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EventTranslatableContentHandler implements TranslatableContentHandler {

    private final EventRepository eventRepository;
    private final EventTranslationRepository eventTranslationRepository;
    private final LanguageCatalog languageCatalog;

    @Override
    public String entityType() {
        return "event";
    }

    @Override
    public Map<String, String> loadSourceTexts(Long entityId) {
        log.debug("Loading source texts for event {}", entityId);
        
        Event event = eventRepository.findById(entityId)
                .orElse(null);
        
        if (event == null) {
            log.warn("Event {} not found for translation", entityId);
            return Map.of();
        }
        
        Map<String, String> texts = new HashMap<>();
        
        if (event.getEventName() != null && !event.getEventName().trim().isEmpty()) {
            texts.put("event_name", event.getEventName());
        }
        
        if (event.getDescription() != null && !event.getDescription().trim().isEmpty()) {
            texts.put("description", event.getDescription());
        }
        
        log.debug("Loaded {} source texts for event {}", texts.size(), entityId);
        return texts;
    }

    @Override
    public void saveTranslation(Long entityId, String languageCode, Map<String, String> translatedTexts, 
                               TranslationSource source, TranslationStatus status) {
        log.info("=== SAVETR ANSLATION START === Event: {}, Lang: {}, Texts: {}", entityId, languageCode, translatedTexts);
        
        // 1. Buscar el idioma por código
        var language = languageCatalog.findByCode(languageCode);
        if (language.isEmpty()) {
            log.error("CRITICAL: Language {} not found for saving translation", languageCode);
            return;
        }
        
        Integer languageId = language.get().getLanguageId();
        log.info("Language found: {} -> ID: {}", languageCode, languageId);
        
        // 2. Buscar traducción existente o crear nueva
        Optional<EventTranslation> existingTranslation = 
                eventTranslationRepository.findByEventAndLanguage(entityId.intValue(), languageId);
        
        EventTranslation translation;
        if (existingTranslation.isPresent()) {
            // Actualizar traducción existente
            translation = existingTranslation.get();
            log.info("Updating existing translation {} for event {}/{}", 
                     translation.getTranslationId(), entityId, languageCode);
        } else {
            // Crear nueva traducción
            translation = EventTranslation.builder()
                    .eventId(entityId.intValue())
                    .languageId(languageId)
                    .source(source)
                    .status(status)
                    .build();
            log.info("Creating NEW translation for event {}/{}", entityId, languageCode);
        }
        
        // 3. Actualizar contenido traducido
        boolean updated = false;
        
        if (translatedTexts.containsKey("event_name")) {
            String translatedName = translatedTexts.get("event_name");
            log.info("Processing event_name: '{}'", translatedName);
            if (translatedName != null && !translatedName.trim().isEmpty()) {
                translation.setTranslatedName(translatedName);
                log.info("Set translatedName: '{}'", translatedName);
                updated = true;
            }
        }
        
        if (translatedTexts.containsKey("description")) {
            String translatedDescription = translatedTexts.get("description");
            log.info("Processing description: '{}'", translatedDescription);
            if (translatedDescription != null && !translatedDescription.trim().isEmpty()) {
                translation.setTranslatedDescription(translatedDescription);
                log.info("Set translatedDescription: '{}'", translatedDescription);
                updated = true;
            }
        }
        
        log.info("Updated flag: {}, Source: {}, Status: {}", updated, source, status);
        
        if (updated) {
            translation.setSource(source);
            translation.setStatus(status);
            
            log.info("CALLING eventTranslationRepository.save()...");
            EventTranslation savedTranslation = eventTranslationRepository.save(translation);
            log.info("✅ SAVED SUCCESSFULLY - Translation ID: {} for event {} in language {}", 
                    savedTranslation.getTranslationId(), entityId, languageCode);
        } else {
            log.warn("❌ NO UPDATES - No translations applied to event {}", entityId);
        }
        
        log.info("=== SAVETRANSLATION END ===");
    }

    @Override
    public Optional<TranslationStatus> findTranslationStatus(Long entityId, String languageCode) {
        log.debug("Finding translation status for event {} in language {}", entityId, languageCode);
        
        var language = languageCatalog.findByCode(languageCode);
        if (language.isEmpty()) {
            return Optional.empty();
        }
        
        return eventTranslationRepository
                .findByEventAndLanguage(entityId.intValue(), language.get().getLanguageId())
                .map(EventTranslation::getStatus);
    }
}