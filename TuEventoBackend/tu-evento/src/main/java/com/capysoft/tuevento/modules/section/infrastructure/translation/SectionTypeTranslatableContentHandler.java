package com.capysoft.tuevento.modules.section.infrastructure.translation;

import com.capysoft.tuevento.modules.section.domain.model.SectionType;
import com.capysoft.tuevento.modules.section.domain.repository.SectionTypeRepository;
import com.capysoft.tuevento.modules.language.application.port.out.TranslatableContentHandler;
import com.capysoft.tuevento.modules.section.domain.model.SectionTypeTranslation;
import com.capysoft.tuevento.modules.section.domain.repository.SectionTypeTranslationRepository;
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
 * Handler para traducir contenido de tipos de sección.
 * Implementa la interfaz para el sistema de traducción usando tabla section_type_translation.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SectionTypeTranslatableContentHandler implements TranslatableContentHandler {

    private final SectionTypeRepository sectionTypeRepository;
    private final SectionTypeTranslationRepository sectionTypeTranslationRepository;
    private final LanguageCatalog languageCatalog;

    @Override
    public String entityType() {
        return "section_type";
    }

    @Override
    public Map<String, String> loadSourceTexts(Long entityId) {
        log.debug("Loading source texts for section type {}", entityId);
        
        // Convert Long to Integer for section type ID
        Integer sectionTypeIdInt = Math.toIntExact(entityId);
        SectionType sectionType = sectionTypeRepository.findById(sectionTypeIdInt)
                .orElse(null);
        
        if (sectionType == null) {
            log.warn("Section type {} not found for translation", entityId);
            return Map.of();
        }
        
        Map<String, String> texts = new HashMap<>();
        
        if (sectionType.getName() != null && !sectionType.getName().trim().isEmpty()) {
            texts.put("name", sectionType.getName());
        }
        
        log.debug("Loaded {} source texts for section type {}", texts.size(), entityId);
        return texts;
    }

    @Override
    public void saveTranslation(Long entityId, String languageCode, Map<String, String> translatedTexts, 
                               TranslationSource source, TranslationStatus status) {
        log.debug("Saving translation for section type {} in language {}: {}", entityId, languageCode, translatedTexts);
        
        // 1. Buscar el idioma por código
        var language = languageCatalog.findByCode(languageCode);
        if (language.isEmpty()) {
            log.warn("Language {} not found for saving translation", languageCode);
            return;
        }
        
        Integer languageId = language.get().getLanguageId();
        Long sectionTypeIdLong = entityId;
        
        // 2. Buscar traducción existente o crear nueva
        Optional<SectionTypeTranslation> existingTranslation = 
                sectionTypeTranslationRepository.findBySectionTypeAndLanguage(sectionTypeIdLong, languageId);
        
        SectionTypeTranslation translation;
        if (existingTranslation.isPresent()) {
            // Actualizar traducción existente
            translation = existingTranslation.get();
            log.debug("Updating existing translation {} for section type {}/{}", 
                     translation.getTranslationId(), entityId, languageCode);
        } else {
            // Crear nueva traducción
            translation = new SectionTypeTranslation();
            translation.setSectionTypeId(Math.toIntExact(entityId));
            translation.setLanguageId(languageId);
            translation.setSource(source);
            translation.setStatus(status);
            log.debug("Creating new translation for section type {}/{}", entityId, languageCode);
        }
        
        // 3. Actualizar contenido traducido
        boolean updated = false;
        
        if (translatedTexts.containsKey("name")) {
            String translatedName = translatedTexts.get("name");
            if (translatedName != null && !translatedName.trim().isEmpty()) {
                translation.setTranslatedName(translatedName);
                updated = true;
            }
        }
        
        if (updated) {
            translation.setSource(source);
            translation.setStatus(status);
            
            SectionTypeTranslation savedTranslation = sectionTypeTranslationRepository.save(translation);
            log.info("Saved translation {} for section type {} in language {}", 
                    savedTranslation.getTranslationId(), entityId, languageCode);
        } else {
            log.debug("No translations applied to section type {}", entityId);
        }
    }

    @Override
    public Optional<TranslationStatus> findTranslationStatus(Long entityId, String languageCode) {
        log.debug("Finding translation status for section type {} in language {}", entityId, languageCode);
        
        var language = languageCatalog.findByCode(languageCode);
        if (language.isEmpty()) {
            return Optional.empty();
        }
        
        return sectionTypeTranslationRepository
                .findBySectionTypeAndLanguage(entityId, language.get().getLanguageId())
                .map(SectionTypeTranslation::getStatus);
    }
}
