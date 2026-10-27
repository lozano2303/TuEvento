package com.capysoft.tuevento.modules.section.interfaces.rest;

import com.capysoft.tuevento.modules.section.domain.model.SectionType;
import com.capysoft.tuevento.modules.section.domain.repository.SectionTypeRepository;
import com.capysoft.tuevento.modules.section.domain.model.SectionTypeTranslation;
import com.capysoft.tuevento.modules.language.domain.model.Language;
import com.capysoft.tuevento.modules.section.domain.repository.SectionTypeTranslationRepository;
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
 * Controller para gestionar traducciones de tipos de sección.
 */
@RestController
@RequestMapping("/api/v1/admin/section-type-translations")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Section Type Translation Management", description = "CRUD de traducciones de tipos de sección")
public class SectionTypeTranslationController {

    private final SectionTypeTranslationRepository sectionTypeTranslationRepository;
    private final SectionTypeRepository sectionTypeRepository;
    private final LanguageRepository languageRepository;
    private final TranslationJobRepository translationJobRepository;

    @Operation(summary = "Get all section types with translations for a language",
               description = "Lista todos los tipos de sección con sus traducciones para el idioma especificado")
    @GetMapping("/{languageCode}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getSectionTypesWithTranslations(
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
            List<SectionType> sectionTypes = sectionTypeRepository.findAll();
            
            List<Map<String, Object>> result = new ArrayList<>();
            for (SectionType sectionType : sectionTypes) {
                Optional<SectionTypeTranslation> translationOpt = 
                        sectionTypeTranslationRepository.findBySectionTypeAndLanguage(sectionType.getSectionTypeId().longValue(), languageId);
                
                // Aplicar filtro
                boolean includeSectionType = false;
                boolean needsReview = false;
                
                if ("all".equals(filter)) {
                    includeSectionType = true;
                } else if ("not-translated".equals(filter)) {
                    includeSectionType = translationOpt.isEmpty() || 
                                      translationOpt.get().getTranslatedName() == null || 
                                      translationOpt.get().getTranslatedName().trim().isEmpty();
                } else if ("translated".equals(filter)) {
                    includeSectionType = translationOpt.isPresent() && 
                                      translationOpt.get().getTranslatedName() != null && 
                                      !translationOpt.get().getTranslatedName().trim().isEmpty();
                } else if ("needs-review".equals(filter)) {
                    if (translationOpt.isEmpty()) {
                        includeSectionType = true;
                        needsReview = true;
                    } else {
                        SectionTypeTranslation trans = translationOpt.get();
                        String original = sectionType.getName();
                        String translated = trans.getTranslatedName();
                        
                        if (translated == null || translated.trim().isEmpty()) {
                            includeSectionType = true;
                            needsReview = true;
                        } else if (original.equals(translated)) {
                            includeSectionType = true;
                            needsReview = true;
                        } else if (original.toLowerCase().trim().equals(translated.toLowerCase().trim())) {
                            includeSectionType = true;
                            needsReview = true;
                        }
                    }
                }
                
                if (!includeSectionType) {
                    continue;
                }
                
                // Check if translation is being generated
                boolean translationGenerating = false;
                if (translationOpt.isEmpty() || translationOpt.get().getTranslatedName() == null) {
                    var existingJob = translationJobRepository.findByEntityAndTargetLanguage(
                            "section_type", sectionType.getSectionTypeId(), languageId);
                    if (existingJob.isPresent()) {
                        var jobStatus = existingJob.get().getStatus();
                        translationGenerating = jobStatus.name().equals("PENDING") || jobStatus.name().equals("PROCESSING");
                    }
                }
                
                Map<String, Object> sectionTypeData = new HashMap<>();
                sectionTypeData.put("sectionTypeId", sectionType.getSectionTypeId());
                sectionTypeData.put("sectionTypeName", sectionType.getName());
                sectionTypeData.put("isActive", sectionType.getActive());
                sectionTypeData.put("translatedName", translationOpt.map(SectionTypeTranslation::getTranslatedName).orElse(null));
                sectionTypeData.put("translationStatus", translationOpt.map(t -> t.getStatus().name()).orElse(null));
                sectionTypeData.put("hasTranslation", translationOpt.isPresent());
                sectionTypeData.put("needsReview", needsReview);
                sectionTypeData.put("translationGenerating", translationGenerating);
                
                result.add(sectionTypeData);
            }
            
            log.info("Retrieved {} section types with translations for language {}", result.size(), languageCode);
            return ResponseEntity.ok(ApiResponse.ok("Section types with translations retrieved", result));
            
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
            Optional<SectionType> sectionTypeOpt = sectionTypeRepository.findById(sectionTypeId);
            if (sectionTypeOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Section type not found with ID: " + sectionTypeId));
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
            
            SectionTypeTranslation translation = sectionTypeTranslationRepository
                    .findBySectionTypeAndLanguage(sectionTypeId.longValue(), languageId)
                    .orElseGet(() -> {
                        SectionTypeTranslation newTranslation = new SectionTypeTranslation();
                        newTranslation.setSectionTypeId(sectionTypeId);
                        newTranslation.setLanguageId(languageId);
                        newTranslation.setSource(TranslationSource.MANUAL);
                        newTranslation.setStatus(TranslationStatus.DRAFT);
                        return newTranslation;
                    });
            
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
            
            SectionTypeTranslation saved = sectionTypeTranslationRepository.save(translation);
            
            log.info("Translation {} for section type {} in language {} by admin", 
                    saved.getTranslationId() == null ? "created" : "updated", 
                    sectionTypeId, languageId);
            
            return ResponseEntity.ok(ApiResponse.ok("Translation saved successfully", saved));
            
        } catch (Exception e) {
            log.error("Error saving section type translation for section type {} in language {}", sectionTypeId, languageId, e);
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
            Optional<SectionTypeTranslation> translationOpt = 
                    sectionTypeTranslationRepository.findBySectionTypeAndLanguage(sectionTypeId.longValue(), languageId);
            
            if (translationOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            sectionTypeTranslationRepository.delete(translationOpt.get());
            
            log.info("Translation deleted for section type {} in language {}", sectionTypeId, languageId);
            return ResponseEntity.ok(ApiResponse.ok("Translation deleted successfully", null));
            
        } catch (Exception e) {
            log.error("Error deleting translation for section type {} in language {}", sectionTypeId, languageId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error deleting translation: " + e.getMessage()));
        }
    }
}
