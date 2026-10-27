package com.capysoft.tuevento.modules.category.interfaces.rest;

import com.capysoft.tuevento.shared.application.translation.TranslationRequests;
import com.capysoft.tuevento.modules.category.domain.model.Category;
import com.capysoft.tuevento.modules.category.domain.model.CategoryTranslation;
import com.capysoft.tuevento.modules.category.domain.repository.CategoryRepository;
import com.capysoft.tuevento.modules.category.domain.repository.CategoryTranslationRepository;
import com.capysoft.tuevento.modules.language.domain.model.Language;
import com.capysoft.tuevento.modules.language.domain.repository.LanguageRepository;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationJobStatus;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationSource;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import com.capysoft.tuevento.shared.interfaces.ApiResponse;
import com.capysoft.tuevento.shared.interfaces.translation.TranslationAdminSupport;
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
 * Controller para gestionar traducciones de categorías. Las reglas del listado y del cuerpo
 * de alta/edición son las comunes de {@link TranslationAdminSupport}.
 */
@RestController
@RequestMapping("/api/v1/admin/category-translations")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Category Translation Management", description = "CRUD de traducciones de categorías")
public class CategoryTranslationController {

    private static final Set<TranslationJobStatus> GENERATING =
            EnumSet.of(TranslationJobStatus.PENDING, TranslationJobStatus.PROCESSING);

    private final CategoryTranslationRepository categoryTranslationRepository;
    private final CategoryRepository categoryRepository;
    private final LanguageRepository languageRepository;
    private final TranslationRequests translationRequests;

    @Operation(summary = "Get all categories with translations for a language",
               description = "Lista todas las categorías con sus traducciones para el idioma especificado")
    @GetMapping("/{languageCode}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getCategoriesWithTranslations(
            @Parameter(description = "Language code (en, fr, pt)", example = "en")
            @PathVariable String languageCode,

            @Parameter(description = "Filter: all, needs-review, translated, not-translated", example = "all")
            @RequestParam(defaultValue = TranslationAdminSupport.FILTER_ALL) String filter) {

        try {
            Optional<Language> languageOpt = languageRepository.findByCode(languageCode.toLowerCase());
            if (languageOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Language not found: " + languageCode));
            }
            Integer languageId = languageOpt.get().getLanguageId();

            List<Map<String, Object>> result = new ArrayList<>();
            for (Category category : categoryRepository.findAll()) {
                Optional<CategoryTranslation> translation =
                        categoryTranslationRepository.findByCategoryAndLanguage(category.getCategoryId(), languageId);
                String translatedName = translation.map(CategoryTranslation::getTranslatedName).orElse(null);
                TranslationAdminSupport.FilterMatch match =
                        TranslationAdminSupport.match(filter, category.getName(), translatedName);
                if (match.included()) {
                    result.add(row(category, translation.orElse(null), match.needsReview(),
                            translatedName == null && isGenerating(category.getCategoryId(), languageId)));
                }
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
            if (categoryRepository.findById(categoryId).isEmpty()) {
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
            translation.setSource(TranslationAdminSupport.sourceOf(translationData));
            translation.setStatus(TranslationAdminSupport.statusOf(translationData));

            CategoryTranslation saved = categoryTranslationRepository.save(translation);
            log.info("Translation saved for category {} in language {} by admin", categoryId, languageId);
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

    /** Hay un trabajo de traducción automática pendiente o en curso para esa categoría e idioma. */
    private boolean isGenerating(Integer categoryId, Integer languageId) {
        return translationRequests.jobStatus("category", categoryId, languageId)
                .map(GENERATING::contains)
                .orElse(false);
    }

    /** Fila del listado; {@code translation} es {@code null} si la categoría no tiene traducción. */
    private static Map<String, Object> row(Category category, CategoryTranslation translation,
                                           boolean needsReview, boolean generating) {
        Map<String, Object> row = new HashMap<>();
        row.put("categoryId", category.getCategoryId());
        row.put("categoryName", category.getName());
        row.put("isActive", category.isActive());
        row.put("parentId", category.getDadId());
        row.put("translatedName", translation != null ? translation.getTranslatedName() : null);
        row.put("translationStatus", translation != null ? translation.getStatus().name() : null);
        row.put("hasTranslation", translation != null);
        row.put("needsReview", needsReview);
        row.put("translationGenerating", generating);
        return row;
    }
}
