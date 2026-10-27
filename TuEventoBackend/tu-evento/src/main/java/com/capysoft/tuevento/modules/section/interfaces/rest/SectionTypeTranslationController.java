package com.capysoft.tuevento.modules.section.interfaces.rest;

import com.capysoft.tuevento.shared.application.translation.TranslationRequests;
import com.capysoft.tuevento.shared.application.translation.LanguageSummary;
import com.capysoft.tuevento.shared.application.translation.LanguageCatalog;
import com.capysoft.tuevento.modules.section.domain.model.SectionType;
import com.capysoft.tuevento.modules.section.domain.model.SectionTypeTranslation;
import com.capysoft.tuevento.modules.section.domain.repository.SectionTypeRepository;
import com.capysoft.tuevento.modules.section.domain.repository.SectionTypeTranslationRepository;
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
 * Controller para gestionar traducciones de tipos de sección. Las reglas del listado y del
 * cuerpo de alta/edición son las comunes de {@link TranslationAdminSupport}.
 */
@RestController
@RequestMapping("/api/v1/admin/section-type-translations")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Section Type Translation Management", description = "CRUD de traducciones de tipos de sección")
public class SectionTypeTranslationController {

    private static final Set<TranslationJobStatus> GENERATING =
            EnumSet.of(TranslationJobStatus.PENDING, TranslationJobStatus.PROCESSING);

    private final SectionTypeTranslationRepository sectionTypeTranslationRepository;
    private final SectionTypeRepository sectionTypeRepository;
    private final LanguageCatalog languageCatalog;
    private final TranslationRequests translationRequests;

    @Operation(summary = "Get all section types with translations for a language",
               description = "Lista todos los tipos de sección con sus traducciones para el idioma especificado")
    @GetMapping("/{languageCode}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getSectionTypesWithTranslations(
            @Parameter(description = "Language code (en, fr, pt)", example = "en")
            @PathVariable String languageCode,

            @Parameter(description = "Filter: all, needs-review, translated, not-translated", example = "all")
            @RequestParam(defaultValue = TranslationAdminSupport.FILTER_ALL) String filter) {

        try {
            Optional<LanguageSummary> language = languageCatalog.findByCode(languageCode.toLowerCase());
            if (language.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Language not found: " + languageCode));
            }
            Integer languageId = language.get().getLanguageId();

            List<Map<String, Object>> rows = new ArrayList<>();
            for (SectionType sectionType : sectionTypeRepository.findAll()) {
                SectionTypeTranslation translation = sectionTypeTranslationRepository
                        .findBySectionTypeAndLanguage(sectionType.getSectionTypeId().longValue(), languageId)
                        .orElse(null);
                String translatedName = translation != null ? translation.getTranslatedName() : null;
                var match = TranslationAdminSupport.match(filter, sectionType.getName(), translatedName);
                if (match.included()) {
                    boolean generating = translatedName == null
                            && isGenerating(sectionType.getSectionTypeId(), languageId);
                    rows.add(row(sectionType, translation, match.needsReview(), generating));
                }
            }

            log.info("Retrieved {} section types with translations for language {}", rows.size(), languageCode);
            return ResponseEntity.ok(ApiResponse.ok("Section types with translations retrieved", rows));

        } catch (Exception e) {
            log.error("Error getting section types with translations for language: {}", languageCode, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error retrieving section types: " + e.getMessage()));
        }
    }

    @Operation(summary = "Create or update section type translation",
               description = "Crea o actualiza la traducción de un tipo de sección")
    @PutMapping("/{sectionTypeId}/language/{languageId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<SectionTypeTranslation>> createOrUpdateTranslation(
            @Parameter(description = "Section Type ID", example = "1")
            @PathVariable Integer sectionTypeId,

            @Parameter(description = "Language ID (2=en, 3=fr, 4=pt)", example = "2")
            @PathVariable Integer languageId,

            @RequestBody Map<String, String> translationData) {

        try {
            String invalid = invalidTarget(sectionTypeId, languageId);
            if (invalid != null) {
                return ResponseEntity.badRequest().body(ApiResponse.error(invalid));
            }

            SectionTypeTranslation translation = sectionTypeTranslationRepository
                    .findBySectionTypeAndLanguage(sectionTypeId.longValue(), languageId)
                    .orElseGet(() -> newTranslation(sectionTypeId, languageId));
            String translatedName = translationData.get("translatedName");
            if (translatedName != null) {
                translation.setTranslatedName(translatedName.trim());
            }
            translation.setSource(TranslationAdminSupport.sourceOf(translationData));
            translation.setStatus(TranslationAdminSupport.statusOf(translationData));

            SectionTypeTranslation saved = sectionTypeTranslationRepository.save(translation);
            log.info("Translation saved for section type {} in language {} by admin", sectionTypeId, languageId);
            return ResponseEntity.ok(ApiResponse.ok("Translation saved successfully", saved));

        } catch (Exception e) {
            log.error("Error saving section type translation for section type {} in language {}",
                    sectionTypeId, languageId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error saving translation: " + e.getMessage()));
        }
    }

    @Operation(summary = "Delete section type translation")
    @DeleteMapping("/{sectionTypeId}/language/{languageId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteTranslation(
            @PathVariable Integer sectionTypeId,
            @PathVariable Integer languageId) {

        try {
            Optional<SectionTypeTranslation> existing =
                    sectionTypeTranslationRepository.findBySectionTypeAndLanguage(sectionTypeId.longValue(), languageId);
            if (existing.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            sectionTypeTranslationRepository.delete(existing.get());
            log.info("Translation deleted for section type {} in language {}", sectionTypeId, languageId);
            return ResponseEntity.ok(ApiResponse.ok("Translation deleted successfully", null));

        } catch (Exception e) {
            log.error("Error deleting translation for section type {} in language {}", sectionTypeId, languageId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error deleting translation: " + e.getMessage()));
        }
    }

    /** Motivo por el que no se puede traducir ese tipo de sección a ese idioma, o {@code null} si se puede. */
    private String invalidTarget(Integer sectionTypeId, Integer languageId) {
        if (sectionTypeRepository.findById(sectionTypeId).isEmpty()) {
            return "Section type not found with ID: " + sectionTypeId;
        }
        Optional<LanguageSummary> language = languageCatalog.findById(languageId);
        if (language.isEmpty()) {
            return "Language not found with ID: " + languageId;
        }
        return "es".equalsIgnoreCase(language.get().getCode())
                ? "Cannot create translation for source language (Spanish)" : null;
    }

    private static SectionTypeTranslation newTranslation(Integer sectionTypeId, Integer languageId) {
        SectionTypeTranslation translation = new SectionTypeTranslation();
        translation.setSectionTypeId(sectionTypeId);
        translation.setLanguageId(languageId);
        translation.setSource(TranslationSource.MANUAL);
        translation.setStatus(TranslationStatus.DRAFT);
        return translation;
    }

    /** Hay un trabajo de traducción automática pendiente o en curso para ese tipo e idioma. */
    private boolean isGenerating(Integer sectionTypeId, Integer languageId) {
        return translationRequests.jobStatus("section_type", sectionTypeId, languageId)
                .map(GENERATING::contains)
                .orElse(false);
    }

    /** Fila del listado; {@code translation} es {@code null} si el tipo no tiene traducción. */
    private static Map<String, Object> row(SectionType sectionType, SectionTypeTranslation translation,
                                           boolean needsReview, boolean generating) {
        Map<String, Object> row = new HashMap<>();
        row.put("sectionTypeId", sectionType.getSectionTypeId());
        row.put("sectionTypeName", sectionType.getName());
        row.put("isActive", sectionType.getActive());
        row.put("translatedName", translation != null ? translation.getTranslatedName() : null);
        row.put("translationStatus", translation != null ? translation.getStatus().name() : null);
        row.put("hasTranslation", translation != null);
        row.put("needsReview", needsReview);
        row.put("translationGenerating", generating);
        return row;
    }
}
