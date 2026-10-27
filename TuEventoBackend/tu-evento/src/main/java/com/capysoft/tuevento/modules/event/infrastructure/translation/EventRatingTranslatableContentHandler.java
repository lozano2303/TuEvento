package com.capysoft.tuevento.modules.event.infrastructure.translation;

import com.capysoft.tuevento.modules.event.domain.model.EventRating;
import com.capysoft.tuevento.modules.event.domain.repository.EventRatingRepository;
import com.capysoft.tuevento.modules.language.application.port.out.TranslatableContentHandler;
import com.capysoft.tuevento.modules.event.domain.model.EventRatingTranslation;
import com.capysoft.tuevento.modules.event.domain.repository.EventRatingTranslationRepository;
import com.capysoft.tuevento.shared.application.translation.LanguageCatalog;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationSource;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Handler para la traducción de contenido de comentarios/ratings de eventos.
 * Maneja la carga de textos originales y el guardado de traducciones para event ratings.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventRatingTranslatableContentHandler implements TranslatableContentHandler {

    private final EventRatingRepository eventRatingRepository;
    private final EventRatingTranslationRepository eventRatingTranslationRepository;
    private final LanguageCatalog languageCatalog;

    @Override
    public String entityType() {
        return "event_rating";
    }

    @Override
    public Map<String, String> loadSourceTexts(Long ratingId) {
        log.info("=== LOAD SOURCE TEXTS === EventRating: {}", ratingId);
        
        Optional<EventRating> ratingOpt = eventRatingRepository.findById(ratingId);
        if (ratingOpt.isEmpty()) {
            log.warn("EventRating not found: {}", ratingId);
            return Map.of();
        }

        EventRating rating = ratingOpt.get();
        Map<String, String> sourceTexts = new HashMap<>();
        
        // Solo incluir comentario si tiene contenido
        if (rating.getComment() != null && !rating.getComment().trim().isEmpty()) {
            sourceTexts.put("comment", rating.getComment());
            log.info("Added comment text: '{}'", rating.getComment());
        }
        
        log.info("=== SOURCE TEXTS LOADED === EventRating: {}, Fields: {}", ratingId, sourceTexts.keySet());
        return sourceTexts;
    }

    @Override
    public void saveTranslation(Long entityId, String languageCode, Map<String, String> translatedTexts, 
                               TranslationSource source, TranslationStatus status) {
        log.info("=== SAVE TRANSLATION START === EventRating: {}, Lang: {}, Texts: {}", 
                entityId, languageCode, translatedTexts);
        
        // 1. Buscar el idioma por código
        var languageOpt = languageCatalog.findByCode(languageCode);
        if (languageOpt.isEmpty()) {
            log.warn("Language not found: {}", languageCode);
            return;
        }
        
        Integer languageId = languageOpt.get().getLanguageId();
        log.info("Found language: {} -> ID: {}", languageCode, languageId);
        
        // 2. Buscar traducción existente
        Optional<EventRatingTranslation> existingOpt = eventRatingTranslationRepository
                .findByRatingAndLanguage(entityId.intValue(), languageId);
        
        EventRatingTranslation translation;
        if (existingOpt.isPresent()) {
            translation = existingOpt.get();
            log.info("Found existing translation ID: {}", translation.getTranslationId());
        } else {
            translation = new EventRatingTranslation();
            translation.setRatingId(entityId.intValue());
            translation.setLanguageId(languageId);
            log.info("Creating new translation for EventRating: {}, Language: {}", entityId, languageId);
        }
        
        // 3. Procesar cada campo traducido
        if (translatedTexts.containsKey("comment")) {
            String translatedComment = translatedTexts.get("comment");
            log.info("Processing comment: '{}'", translatedComment);
            
            if (translatedComment != null && !translatedComment.trim().isEmpty()) {
                translation.setTranslatedComment(translatedComment);
                log.info("Set translated comment");
            }
        }
        
        // 4. Establecer metadatos (status es manejado por el TranslationProcessor)
        translation.setStatus(status);
        
        // 5. Guardar
        eventRatingTranslationRepository.save(translation);
        log.info("=== TRANSLATION SAVED === EventRating: {}, Lang: {}", entityId, languageCode);
    }

    @Override
    public Optional<TranslationStatus> findTranslationStatus(Long entityId, String languageCode) {
        var languageOpt = languageCatalog.findByCode(languageCode);
        if (languageOpt.isEmpty()) {
            return Optional.empty();
        }
        
        return eventRatingTranslationRepository
                .findByRatingAndLanguage(entityId.intValue(), languageOpt.get().getLanguageId())
                .map(EventRatingTranslation::getStatus);
    }
}
