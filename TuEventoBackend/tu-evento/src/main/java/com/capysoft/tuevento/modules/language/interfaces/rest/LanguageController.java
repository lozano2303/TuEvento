package com.capysoft.tuevento.modules.language.interfaces.rest;

import com.capysoft.tuevento.modules.language.application.dto.request.CreateLanguageRequest;
import com.capysoft.tuevento.modules.language.application.dto.request.UpdateLanguageRequest;
import com.capysoft.tuevento.modules.language.application.dto.response.LanguageResponse;
import com.capysoft.tuevento.modules.language.application.port.in.CreateLanguageUseCase;
import com.capysoft.tuevento.modules.language.application.port.in.GetLanguagesUseCase;
import com.capysoft.tuevento.modules.language.application.port.in.UpdateLanguageUseCase;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import com.capysoft.tuevento.shared.interfaces.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para gestión de idiomas.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Languages", description = "Language management endpoints")
public class LanguageController {

    private final CreateLanguageUseCase createLanguageUseCase;
    private final GetLanguagesUseCase getLanguagesUseCase;
    private final UpdateLanguageUseCase updateLanguageUseCase;

    /**
     * Obtiene todos los idiomas activos (público).
     */
    @GetMapping("/api/v1/languages")
    @Operation(summary = "Get active languages", description = "Returns all active languages")
    public ResponseEntity<ApiResponse<List<LanguageResponse>>> getActiveLanguages() {
        List<LanguageResponse> languages = getLanguagesUseCase.getAllActiveLanguages();
        return ResponseEntity.ok(ApiResponse.ok("Active languages retrieved successfully", languages));
    }

    /**
     * Obtiene un idioma por ID (público).
     */
    @GetMapping("/api/v1/languages/{id}")
    @Operation(summary = "Get language by ID", description = "Returns a language by its ID")
    public ResponseEntity<ApiResponse<LanguageResponse>> getLanguageById(@PathVariable Integer id) {
        LanguageResponse language = getLanguagesUseCase.getLanguageById(id)
                .orElseThrow(() -> new NotFoundException("LANGUAGE_NOT_FOUND", "Language not found with id: " + id));
        
        return ResponseEntity.ok(ApiResponse.ok("Language retrieved successfully", language));
    }

    /**
     * Obtiene todos los idiomas (admin).
     */
    @GetMapping("/api/v1/admin/languages")
    @Operation(summary = "Get all languages", description = "Returns all languages (active and inactive) - Admin only")
    public ResponseEntity<ApiResponse<List<LanguageResponse>>> getAllLanguages() {
        List<LanguageResponse> languages = getLanguagesUseCase.getAllLanguages();
        return ResponseEntity.ok(ApiResponse.ok("All languages retrieved successfully", languages));
    }

    /**
     * Crea un nuevo idioma (admin).
     */
    @PostMapping("/api/v1/admin/languages")
    @Operation(summary = "Create language", description = "Creates a new language - Admin only")
    public ResponseEntity<ApiResponse<LanguageResponse>> createLanguage(@Valid @RequestBody CreateLanguageRequest request) {
        LanguageResponse language = createLanguageUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Language created successfully", language));
    }

    /**
     * Actualiza un idioma (admin).
     */
    @PutMapping("/api/v1/admin/languages/{id}")
    @Operation(summary = "Update language", description = "Updates a language - Admin only")
    public ResponseEntity<ApiResponse<LanguageResponse>> updateLanguage(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateLanguageRequest request) {
        LanguageResponse language = updateLanguageUseCase.updateLanguage(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Language updated successfully", language));
    }

    /**
     * Activa un idioma (admin).
     */
    @PatchMapping("/api/v1/admin/languages/{id}/activate")
    @Operation(summary = "Activate language", description = "Activates a language - Admin only")
    public ResponseEntity<ApiResponse<LanguageResponse>> activateLanguage(@PathVariable Integer id) {
        LanguageResponse language = updateLanguageUseCase.activateLanguage(id);
        return ResponseEntity.ok(ApiResponse.ok("Language activated successfully", language));
    }

    /**
     * Desactiva un idioma (admin).
     */
    @PatchMapping("/api/v1/admin/languages/{id}/deactivate")
    @Operation(summary = "Deactivate language", description = "Deactivates a language - Admin only")
    public ResponseEntity<ApiResponse<LanguageResponse>> deactivateLanguage(@PathVariable Integer id) {
        LanguageResponse language = updateLanguageUseCase.deactivateLanguage(id);
        return ResponseEntity.ok(ApiResponse.ok("Language deactivated successfully", language));
    }

    /**
     * Establece un idioma como el por defecto (admin).
     */
    @PostMapping("/api/v1/admin/languages/{id}/set-default")
    @Operation(summary = "Set default language", description = "Sets a language as the default language - Admin only")
    public ResponseEntity<ApiResponse<LanguageResponse>> setDefaultLanguage(@PathVariable Integer id) {
        LanguageResponse language = updateLanguageUseCase.setDefaultLanguage(id);
        return ResponseEntity.ok(ApiResponse.ok("Default language set successfully", language));
    }
}