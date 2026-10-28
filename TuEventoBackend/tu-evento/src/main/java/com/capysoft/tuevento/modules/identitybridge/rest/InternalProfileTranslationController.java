package com.capysoft.tuevento.modules.identitybridge.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.capysoft.tuevento.shared.application.translation.TranslationRequests;
import com.capysoft.tuevento.shared.interfaces.ApiResponse;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;

/**
 * API interna para identity-service, que ya atiende los perfiles pero no tiene el núcleo de
 * traducción: consulta aquí en qué estado está la traducción de una biografía. No la usan web ni
 * móvil y el gateway no la enruta; exige la clave entre servicios ({@code InternalApiKeyFilter}).
 */
@Hidden
@RestController
@RequestMapping("/internal/profiles")
@RequiredArgsConstructor
public class InternalProfileTranslationController {

    static final String ENTITY_TYPE = "profile";

    private final TranslationRequests translationRequests;

    /** {@code NONE} si nunca se pidió; si no, en curso, completado o fallido. */
    public record JobStateResponse(String state) {
    }

    @GetMapping("/{profileId}/translation-jobs/{languageId}")
    public ResponseEntity<ApiResponse<JobStateResponse>> getJobState(@PathVariable Long profileId,
                                                                     @PathVariable Integer languageId) {
        String state = translationRequests.jobStatus(ENTITY_TYPE, profileId, languageId)
                .map(status -> switch (status) {
                    case PENDING, PROCESSING -> "IN_PROGRESS";
                    case COMPLETED -> "COMPLETED";
                    case FAILED -> "FAILED";
                })
                .orElse("NONE");
        return ResponseEntity.ok(ApiResponse.ok("Translation job state retrieved", new JobStateResponse(state)));
    }
}
