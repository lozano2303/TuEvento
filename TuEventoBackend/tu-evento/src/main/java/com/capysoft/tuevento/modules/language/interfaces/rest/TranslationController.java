package com.capysoft.tuevento.modules.language.interfaces.rest;

import com.capysoft.tuevento.modules.language.application.service.TranslationService;
import com.capysoft.tuevento.modules.language.domain.model.TranslationJob;
import com.capysoft.tuevento.modules.language.domain.repository.TranslationJobRepository;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationJobStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller para testing y administración del sistema de traducción.
 */
@RestController
@RequestMapping("/api/admin/translations")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Translation Management", description = "Endpoints para administrar traducciones")
public class TranslationController {

    private final TranslationService translationService;
    private final TranslationJobRepository translationJobRepository;

    @Operation(summary = "Request translation for an entity", 
               description = "Manually request translation for a specific entity")
    @PostMapping("/request")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<String> requestTranslation(
            @Parameter(description = "Entity type (event, category, etc.)")
            @RequestParam String entityType,
            
            @Parameter(description = "Entity ID") 
            @RequestParam Long entityId,
            
            @Parameter(description = "Source language code", example = "es")
            @RequestParam(defaultValue = "es") String sourceLanguageCode) {
        
        try {
            translationService.requestTranslation(entityType, entityId, sourceLanguageCode);
            return ResponseEntity.ok("Translation requested successfully for " + entityType + " " + entityId);
        } catch (Exception e) {
            log.error("Failed to request translation", e);
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @Operation(summary = "Get translation jobs by entity", 
               description = "Get all translation jobs for a specific entity")
    @GetMapping("/jobs/{entityType}/{entityId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<TranslationJob>> getTranslationJobsByEntity(
            @Parameter(description = "Entity type") 
            @PathVariable String entityType,
            
            @Parameter(description = "Entity ID")
            @PathVariable Long entityId) {
        
        List<TranslationJob> jobs = translationJobRepository.findByEntityAndStatus(entityType, entityId, null);
        return ResponseEntity.ok(jobs);
    }

    @Operation(summary = "Get pending translation jobs", 
               description = "Get all pending translation jobs for debugging")
    @GetMapping("/jobs/pending")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<TranslationJob>> getPendingJobs(
            @Parameter(description = "Entity type filter")
            @RequestParam(required = false) String entityType,
            
            @Parameter(description = "Entity ID filter")
            @RequestParam(required = false) Long entityId) {
        
        List<TranslationJob> jobs;
        
        if (entityType != null && entityId != null) {
            jobs = translationJobRepository.findByEntityAndStatus(entityType, entityId, TranslationJobStatus.PENDING);
        } else {
            // Find all pending jobs - we'll need to add a method for this
            jobs = List.of(); // TODO: Add findAllByStatus method to repository
        }
        
        return ResponseEntity.ok(jobs);
    }
}