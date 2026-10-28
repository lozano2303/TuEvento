package com.capysoft.identity.profile.interfaces.rest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.capysoft.common.domain.exception.BusinessException;
import com.capysoft.common.interfaces.ApiResponse;
import com.capysoft.identity.profile.application.port.out.LanguageDirectoryPort;
import com.capysoft.identity.profile.domain.model.Profile;
import com.capysoft.identity.profile.domain.model.ProfileTranslation;
import com.capysoft.identity.profile.domain.repository.ProfileRepository;
import com.capysoft.identity.profile.domain.repository.ProfileTranslationRepository;
import com.capysoft.identity.shared.domain.valueobject.TranslationSource;
import com.capysoft.identity.shared.domain.valueobject.TranslationStatus;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * API interna de perfiles para el monolito, que conserva el motor de traducciones y la subida del
 * avatar por defecto: lee la biografía a traducir, guarda la traducción y asigna el avatar por
 * defecto. No la usan web ni móvil y el gateway no la enruta; exige la clave entre servicios.
 */
@Slf4j
@Hidden
@RestController
@RequestMapping("/internal/profiles")
@RequiredArgsConstructor
public class InternalProfileController {

    private final ProfileRepository profileRepository;
    private final ProfileTranslationRepository profileTranslationRepository;
    private final LanguageDirectoryPort languageDirectoryPort;

    public record TranslationRequest(Map<String, String> texts, TranslationSource source, TranslationStatus status) {
    }

    public record TranslationStatusResponse(TranslationStatus status) {
    }

    /** Textos traducibles del perfil (hoy solo la biografía); vacío si no tiene o no existe. */
    @GetMapping("/{profileId}/translatable-texts")
    public ResponseEntity<ApiResponse<Map<String, String>>> translatableTexts(@PathVariable Long profileId) {
        Map<String, String> texts = new HashMap<>();
        profileRepository.findById(profileId)
                .map(Profile::getBio)
                .filter(bio -> !bio.trim().isEmpty())
                .ifPresent(bio -> texts.put("bio", bio));
        return ResponseEntity.ok(ApiResponse.ok("Translatable texts retrieved", texts));
    }

    /** Crea o actualiza la traducción del perfil en ese idioma (la escribe el motor de traducciones). */
    @PutMapping("/{profileId}/translations/{languageCode}")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> saveTranslation(@PathVariable Long profileId,
                                                             @PathVariable String languageCode,
                                                             @RequestBody TranslationRequest request) {
        Integer languageId = languageDirectoryPort.findIdByCode(languageCode)
                .orElseThrow(() -> new BusinessException("LANGUAGE_NOT_FOUND", "Language not found: " + languageCode));
        ProfileTranslation translation = profileTranslationRepository.findByProfileAndLanguage(profileId, languageId)
                .orElseGet(() -> {
                    ProfileTranslation created = new ProfileTranslation();
                    created.setProfileId(profileId.intValue());
                    created.setLanguageId(languageId);
                    return created;
                });
        String bio = request.texts() == null ? null : request.texts().get("bio");
        if (bio != null && !bio.trim().isEmpty()) {
            translation.setTranslatedBio(bio);
        }
        translation.setSource(request.source());
        translation.setStatus(request.status());
        profileTranslationRepository.save(translation);
        return ResponseEntity.ok(ApiResponse.ok("Translation saved"));
    }

    @GetMapping("/{profileId}/translations/{languageCode}/status")
    public ResponseEntity<ApiResponse<TranslationStatusResponse>> translationStatus(@PathVariable Long profileId,
                                                                                    @PathVariable String languageCode) {
        TranslationStatus status = languageDirectoryPort.findIdByCode(languageCode)
                .flatMap(languageId -> profileTranslationRepository.findByProfileAndLanguage(profileId, languageId))
                .map(ProfileTranslation::getStatus)
                .orElse(null);
        return ResponseEntity.ok(ApiResponse.ok("Translation status retrieved", new TranslationStatusResponse(status)));
    }

    /** Asigna el avatar por defecto (que sube el monolito a Archivos) a los perfiles que no tienen. */
    @PutMapping("/default-avatar")
    @Transactional
    public ResponseEntity<ApiResponse<Integer>> assignDefaultAvatar(@RequestParam Integer storedFileId) {
        List<Profile> profiles = profileRepository.findAllWithNullStoredFileId();
        for (Profile profile : profiles) {
            profile.setStoredFileId(storedFileId);
            profileRepository.save(profile);
        }
        if (!profiles.isEmpty()) {
            log.info("Default avatar (storedFileId={}) assigned to {} profile(s)", storedFileId, profiles.size());
        }
        return ResponseEntity.ok(ApiResponse.ok("Default avatar assigned", profiles.size()));
    }
}
