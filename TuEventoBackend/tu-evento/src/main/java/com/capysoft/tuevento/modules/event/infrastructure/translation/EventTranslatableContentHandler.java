package com.capysoft.tuevento.modules.event.infrastructure.translation;

import com.capysoft.tuevento.modules.event.domain.model.Event;
import com.capysoft.tuevento.modules.event.domain.repository.EventRepository;
import com.capysoft.tuevento.modules.language.application.port.out.TranslatableContentHandler;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationSource;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Handler para traducir contenido de eventos.
 * Implementa la interfaz para el sistema de traducción.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EventTranslatableContentHandler implements TranslatableContentHandler {

    private final EventRepository eventRepository;

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
        
        Event event = eventRepository.findById(entityId)
                .orElse(null);
        
        if (event == null) {
            log.warn("Event {} not found for saving translation", entityId);
            return;
        }
        
        // TODO: For now, apply directly to source entity. 
        // Later: implement proper translation storage with language versions
        boolean updated = false;
        
        if (translatedTexts.containsKey("event_name")) {
            String translatedName = translatedTexts.get("event_name");
            if (translatedName != null && !translatedName.trim().isEmpty()) {
                event.setEventName(translatedName);
                updated = true;
            }
        }
        
        if (translatedTexts.containsKey("description")) {
            String translatedDescription = translatedTexts.get("description");
            if (translatedDescription != null && !translatedDescription.trim().isEmpty()) {
                event.setDescription(translatedDescription);
                updated = true;
            }
        }
        
        if (updated) {
            eventRepository.save(event);
            log.info("Applied translation for event {} in language {}", entityId, languageCode);
        } else {
            log.debug("No translations applied to event {}", entityId);
        }
    }

    @Override
    public Optional<TranslationStatus> findTranslationStatus(Long entityId, String languageCode) {
        // TODO: Implement proper translation status lookup
        // For now, always return empty - translations will be created
        log.debug("Finding translation status for event {} in language {} (not implemented)", entityId, languageCode);
        return Optional.empty();
    }
}