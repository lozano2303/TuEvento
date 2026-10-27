package com.capysoft.tuevento.modules.category.interfaces.rest;

import com.capysoft.tuevento.modules.category.domain.model.Category;
import com.capysoft.tuevento.modules.category.domain.repository.CategoryRepository;
import com.capysoft.tuevento.modules.category.domain.model.CategoryTranslation;
import com.capysoft.tuevento.modules.language.domain.model.Language;
import com.capysoft.tuevento.modules.category.domain.repository.CategoryTranslationRepository;
import com.capysoft.tuevento.modules.language.domain.repository.LanguageRepository;
import com.capysoft.tuevento.modules.language.domain.repository.TranslationJobRepository;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationSource;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import com.capysoft.tuevento.shared.interfaces.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Controller para gestionar traducciones de categorías.
 */
@RestController
@RequestMapping("/api/v1/admin/category-translations")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Category Translation Management", description = "CRUD de traducciones de categorías")
public class CategoryTranslationController {

    private final CategoryTranslationRepository categoryTranslationRepository;
    private final CategoryRepository categoryRepository;
    private final LanguageRepository languageRepository;
    private final TranslationJobRepository translationJobRepository;

    @Operation(summary = "Get all categories with translations for a language",
               description = "Lista todas las categorías con sus traducciones para el idioma especificado")
    @GetMapping("/{languageCode}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getCategoriesWithTranslations(
            @Parameter(description = "Language code (en, fr, pt)", example = "en") 
            @PathVariable String languageCode,
            
            @Parameter(description = "Filter: all, needs-review, translated, not-translated", example = "all")
            @RequestParam(defaultValue = "all") String filter) {
        
        try {
            Optional<Language> languageOpt = languageRepository.findByCode(languageCode.toLowerCase());
            if (languageOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Language not found: " + languageCode));
            }
            
            Integer languageId = languageOpt.get().getLanguageId();
            List<Category> categories = categoryRepository.findAll();
            
            List<Map<String, Object>> result = new ArrayList<>();
            for (Category category : categories) {
                Optional<CategoryTranslation> translationOpt = 
                        categoryTranslationRepository.findByCategoryAndLanguage(category.getCategoryId(), languageId);
                
                // Aplicar filtro
                boolean includeCategory = false;
                boolean needsReview = false;
                
                if ("all".equals(filter)) {
                    includeCategory = true;
                } else if ("not-translated".equals(filter)) {
                    includeCategory = translationOpt.isEmpty() || 
                                      translationOpt.get().getTranslatedName() == null || 
                                      translationOpt.get().getTranslatedName().trim().isEmpty();
                } else if ("translated".equals(filter)) {
                    includeCategory = translationOpt.isPresent() && 
                                      translationOpt.get().getTranslatedName() != null && 
                                      !translationOpt.get().getTranslatedName().trim().isEmpty();
                } else if ("needs-review".equals(filter)) {
                    if (translationOpt.isEmpty()) {
                        includeCategory = true;
                        needsReview = true;
                    } else {
                        CategoryTranslation trans = translationOpt.get();
                        String original = category.getName();
                        String translated = trans.getTranslatedName();
                        
                        if (translated == null || translated.trim().isEmpty()) {
                            includeCategory = true;
                            needsReview = true;
                        } else if (original.equals(translated)) {
                            includeCategory = true;
                            needsReview = true;
                        } else if (original.toLowerCase().trim().equals(translated.toLowerCase().trim())) {
                            includeCategory = true;
                            needsReview = true;
                        }
                    }
                }
                
                if (!includeCategory) {
                    continue;
                }
                
                // Check if translation is being generated
                boolean translationGenerating = false;
                if (translationOpt.isEmpty() || translationOpt.get().getTranslatedName() == null) {
                    var existingJob = translationJobRepository.findByEntityAndTargetLanguage(
                            "category", category.getCategoryId(), languageId);
                    if (existingJob.isPresent()) {
                        var jobStatus = existingJob.get().getStatus();
                        translationGenerating = jobStatus.name().equals("PENDING") || jobStatus.name().equals("PROCESSING");
                    }
                }
                
                Map<String, Object> categoryData = new HashMap<>();
                categoryData.put("categoryId", category.getCategoryId());
                categoryData.put("categoryName", category.getName());
                categoryData.put("isActive", category.isActive());
                categoryData.put("parentId", category.getDadId());
                categoryData.put("translatedName", translationOpt.map(CategoryTranslation::getTranslatedName).orElse(null));
                categoryData.put("translationStatus", translationOpt.map(t -> t.getStatus().name()).orElse(null));
                categoryData.put("hasTranslation", translationOpt.isPresent());
                categoryData.put("needsReview", needsReview);
                categoryData.put("translationGenerating", translationGenerating);
                
                result.add(categoryData);
            }
            
            log.info("Retrieved {} categories with translations for language {}", result.size(), languageCode);
            return ResponseEntity.ok(ApiResponse.ok("Categories with translations retrieved", result));
            
        } catch (Exception e) {
            log.error("Error getting categories with translations for language: {}", languageCode, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error retrieving categories: " + e.getMessage()));
        }
    }

    @Operation(summary = "Create or update category translation",
               description = "Crea o actualiza la traducción de una categoría")
    @PutMapping("/{categoryId}/language/{languageId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryTranslation>> createOrUpdateTranslation(
            @Parameter(description = "Category ID", example = "1") 
            @PathVariable Integer categoryId,
            
            @Parameter(description = "Language ID (2=en, 3=fr, 4=pt)", example = "2") 
            @PathVariable Integer languageId,
            
            @RequestBody Map<String, String> translationData) {
        
        try {
            Optional<Category> categoryOpt = categoryRepository.findById(categoryId);
            if (categoryOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Category not found with ID: " + categoryId));
            }
            
            Optional<Language> languageOpt = languageRepository.findById(languageId);
            if (languageOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Language not found with ID: " + languageId));
            }
            
            if ("es".equalsIgnoreCase(languageOpt.get().getCode())) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Cannot create translation for source language (Spanish)"));
            }
            
            CategoryTranslation translation = categoryTranslationRepository
                    .findByCategoryAndLanguage(categoryId, languageId)
                    .orElse(CategoryTranslation.builder()
                            .categoryId(categoryId)
                            .languageId(languageId)
                            .source(TranslationSource.MANUAL)
                            .status(TranslationStatus.DRAFT)
                            .build());
            
            String translatedName = translationData.get("translatedName");
            if (translatedName != null) {
                translation.setTranslatedName(translatedName.trim());
            }
            
            if (translationData.containsKey("source")) {
                try {
                    translation.setSource(TranslationSource.valueOf(translationData.get("source")));
                } catch (IllegalArgumentException e) {
                    translation.setSource(TranslationSource.MANUAL);
                }
            } else {
                translation.setSource(TranslationSource.MANUAL);
            }
            
            if (translationData.containsKey("status")) {
                try {
                    translation.setStatus(TranslationStatus.valueOf(translationData.get("status")));
                } catch (IllegalArgumentException e) {
                    translation.setStatus(TranslationStatus.PUBLISHED);
                }
            } else {
                translation.setStatus(TranslationStatus.PUBLISHED);
            }
            
            CategoryTranslation saved = categoryTranslationRepository.save(translation);
            
            log.info("Translation {} for category {} in language {} by admin", 
                    saved.getTranslationId() == null ? "created" : "updated", 
                    categoryId, languageId);
            
            return ResponseEntity.ok(ApiResponse.ok("Translation saved successfully", saved));
            
        } catch (Exception e) {
            log.error("Error saving category translation for category {} in language {}", categoryId, languageId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error saving translation: " + e.getMessage()));
        }
    }

    @Operation(summary = "Delete category translation")
    @DeleteMapping("/{categoryId}/language/{languageId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteTranslation(
            @PathVariable Integer categoryId,
            @PathVariable Integer languageId) {
        
        try {
            Optional<CategoryTranslation> translationOpt = 
                    categoryTranslationRepository.findByCategoryAndLanguage(categoryId, languageId);
            
            if (translationOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            categoryTranslationRepository.delete(translationOpt.get());
            
            log.info("Translation deleted for category {} in language {}", categoryId, languageId);
            return ResponseEntity.ok(ApiResponse.ok("Translation deleted successfully", null));
            
        } catch (Exception e) {
            log.error("Error deleting translation for category {} in language {}", categoryId, languageId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error deleting translation: " + e.getMessage()));
        }
    }
}
