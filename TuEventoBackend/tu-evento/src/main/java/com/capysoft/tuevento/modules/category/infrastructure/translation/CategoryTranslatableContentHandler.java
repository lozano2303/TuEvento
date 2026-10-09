package com.capysoft.tuevento.modules.category.infrastructure.translation;

import com.capysoft.tuevento.modules.category.domain.model.Category;
import com.capysoft.tuevento.modules.category.domain.repository.CategoryRepository;
import com.capysoft.tuevento.modules.language.application.port.out.TranslatableContentHandler;
import com.capysoft.tuevento.modules.language.domain.model.CategoryTranslation;
import com.capysoft.tuevento.modules.language.domain.repository.CategoryTranslationRepository;
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
 * Handler para traducir contenido de categorías.
 * Implementa la interfaz para el sistema de traducción usando tabla category_translation.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CategoryTranslatableContentHandler implements TranslatableContentHandler {

    private final CategoryRepository categoryRepository;
    private final CategoryTranslationRepository categoryTranslationRepository;
    private final LanguageRepository languageRepository;

    @Override
    public String entityType() {
        return "category";
    }

    @Override
    public Map<String, String> loadSourceTexts(Long entityId) {
        log.debug("Loading source texts for category {}", entityId);
        
        // Convert Long to Integer for category ID
        Integer categoryIdInt = Math.toIntExact(entityId);
        Category category = categoryRepository.findById(categoryIdInt)
                .orElse(null);
        
        if (category == null) {
            log.warn("Category {} not found for translation", entityId);
            return Map.of();
        }
        
        Map<String, String> texts = new HashMap<>();
        
        if (category.getName() != null && !category.getName().trim().isEmpty()) {
            texts.put("name", category.getName());
        }
        
        if (category.getDescription() != null && !category.getDescription().trim().isEmpty()) {
            texts.put("description", category.getDescription());
        }
        
        log.debug("Loaded {} source texts for category {}", texts.size(), entityId);
        return texts;
    }

    @Override
    public void saveTranslation(Long entityId, String languageCode, Map<String, String> translatedTexts, 
                               TranslationSource source, TranslationStatus status) {
        log.debug("Saving translation for category {} in language {}: {}", entityId, languageCode, translatedTexts);
        
        // 1. Buscar el idioma por código
        var language = languageRepository.findByCode(languageCode);
        if (language.isEmpty()) {
            log.warn("Language {} not found for saving translation", languageCode);
            return;
        }
        
        Long languageId = language.get().getLanguageId();
        Integer categoryIdInt = Math.toIntExact(entityId);
        
        // 2. Buscar traducción existente o crear nueva
        Optional<CategoryTranslation> existingTranslation = 
                categoryTranslationRepository.findByCategoryAndLanguage(categoryIdInt, languageId);
        
        CategoryTranslation translation;
        if (existingTranslation.isPresent()) {
            // Actualizar traducción existente
            translation = existingTranslation.get();
            log.debug("Updating existing translation {} for category {}/{}", 
                     translation.getTranslationId(), entityId, languageCode);
        } else {
            // Crear nueva traducción
            translation = CategoryTranslation.builder()
                    .categoryId(categoryIdInt)
                    .languageId(languageId.intValue())
                    .source(source)
                    .status(status)
                    .build();
            log.debug("Creating new translation for category {}/{}", entityId, languageCode);
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
            
            CategoryTranslation savedTranslation = categoryTranslationRepository.save(translation);
            log.info("Saved translation {} for category {} in language {}", 
                    savedTranslation.getTranslationId(), entityId, languageCode);
        } else {
            log.debug("No translations applied to category {}", entityId);
        }
    }

    @Override
    public Optional<TranslationStatus> findTranslationStatus(Long entityId, String languageCode) {
        log.debug("Finding translation status for category {} in language {}", entityId, languageCode);
        
        var language = languageRepository.findByCode(languageCode);
        if (language.isEmpty()) {
            return Optional.empty();
        }
        
        Integer categoryIdInt = Math.toIntExact(entityId);
        return categoryTranslationRepository
                .findByCategoryAndLanguage(categoryIdInt, language.get().getLanguageId())
                .map(CategoryTranslation::getStatus);
    }
}