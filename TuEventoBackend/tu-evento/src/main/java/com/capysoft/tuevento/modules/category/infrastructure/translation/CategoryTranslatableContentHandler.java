package com.capysoft.tuevento.modules.category.infrastructure.translation;

import com.capysoft.tuevento.modules.category.domain.model.Category;
import com.capysoft.tuevento.modules.category.domain.repository.CategoryRepository;
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
 * Handler para traducir contenido de categorías.
 * Implementa la interfaz para el sistema de traducción.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CategoryTranslatableContentHandler implements TranslatableContentHandler {

    private final CategoryRepository categoryRepository;

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
        
        // Convert Long to Integer for category ID
        Integer categoryIdInt = Math.toIntExact(entityId);
        Category category = categoryRepository.findById(categoryIdInt)
                .orElse(null);
        
        if (category == null) {
            log.warn("Category {} not found for saving translation", entityId);
            return;
        }
        
        // TODO: For now, apply directly to source entity.
        // Later: implement proper translation storage with language versions
        boolean updated = false;
        
        if (translatedTexts.containsKey("name")) {
            String translatedName = translatedTexts.get("name");
            if (translatedName != null && !translatedName.trim().isEmpty()) {
                category.setName(translatedName);
                updated = true;
            }
        }
        
        if (translatedTexts.containsKey("description")) {
            String translatedDescription = translatedTexts.get("description");
            if (translatedDescription != null && !translatedDescription.trim().isEmpty()) {
                category.setDescription(translatedDescription);
                updated = true;
            }
        }
        
        if (updated) {
            categoryRepository.save(category);
            log.info("Applied translation for category {} in language {}", entityId, languageCode);
        } else {
            log.debug("No translations applied to category {}", entityId);
        }
    }

    @Override
    public Optional<TranslationStatus> findTranslationStatus(Long entityId, String languageCode) {
        // TODO: Implement proper translation status lookup
        // For now, always return empty - translations will be created
        log.debug("Finding translation status for category {} in language {} (not implemented)", entityId, languageCode);
        return Optional.empty();
    }
}