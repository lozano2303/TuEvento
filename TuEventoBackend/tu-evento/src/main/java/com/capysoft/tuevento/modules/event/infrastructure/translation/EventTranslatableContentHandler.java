package com.capysoft.tuevento.modules.event.infrastructure.translation;

import com.capysoft.tuevento.modules.event.domain.model.Event;
import com.capysoft.tuevento.modules.event.domain.repository.EventRepository;
import com.capysoft.tuevento.modules.language.application.port.out.TranslatableContentHandler;
import com.capysoft.tuevento.modules.language.domain.model.EventTranslation;
import com.capysoft.tuevento.modules.language.domain.repository.EventTranslationRepository;
import com.capysoft.tuevento.modules.language.domain.repository.LanguageRepository;
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
    private final LanguageRepository languageRepository;

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
        log.debug("Saving translation for event {} in language {}: {}", entityId, languageCode, translatedTexts);
        
        // 1. Buscar el idioma por código
        var language = languageRepository.findByCode(languageCode);
        if (language.isEmpty()) {
            log.warn("Language {} not found for saving translation", languageCode);
            return;
        }
        
        Long languageId = language.get().getLanguageId();
        
        // 2. Buscar traducción existente o crear nueva
        Optional<EventTranslation> existingTranslation = 
                eventTranslationRepository.findByEventAndLanguage(entityId, languageId);
        
        EventTranslation translation;
        if (existingTranslation.isPresent()) {
            // Actualizar traducción existente
            translation = existingTranslation.get();
            log.debug("Updating existing translation {} for event {}/{}", 
                     translation.getTranslationId(), entityId, languageCode);
        } else {
            // Crear nueva traducción
            translation = EventTranslation.builder()
                    .eventId(entityId)
                    .languageId(languageId.intValue())
                    .source(source)
                    .status(status)
                    .build();
            log.debug("Creating new translation for event {}/{}", entityId, languageCode);
        }
        
        // 3. Actualizar contenido traducido
        boolean updated = false;
        
        if (translatedTexts.containsKey("event_name")) {
            String translatedName = translatedTexts.get("event_name");
            if (translatedName != null && !translatedName.trim().isEmpty()) {
                translation.setTranslatedName(translatedName);
                updated = true;
            }
        }
        
        if (translatedTexts.containsKey("description")) {
            String translatedDescription = translatedTexts.get("description");
            if (translatedDescription != null && !translatedDescription.trim().isEmpty()) {
                translation.setTranslatedDescription(translatedDescription);
                updated = true;
            }
        }
        
        if (updated) {
            translation.setSource(source);
            translation.setStatus(status);
            
            EventTranslation savedTranslation = eventTranslationRepository.save(translation);
            log.info("Saved translation {} for event {} in language {}", 
                    savedTranslation.getTranslationId(), entityId, languageCode);
        } else {
            log.debug("No translations applied to event {}", entityId);
        }
    }

    @Override
    public Optional<TranslationStatus> findTranslationStatus(Long entityId, String languageCode) {
        log.debug("Finding translation status for event {} in language {}", entityId, languageCode);
        
        var language = languageRepository.findByCode(languageCode);
        if (language.isEmpty()) {
            return Optional.empty();
        }
        
        return eventTranslationRepository
                .findByEventAndLanguage(entityId, language.get().getLanguageId())
                .map(EventTranslation::getStatus);
    }
}