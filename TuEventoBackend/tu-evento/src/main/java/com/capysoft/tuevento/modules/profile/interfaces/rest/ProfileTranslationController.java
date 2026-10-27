package com.capysoft.tuevento.modules.profile.interfaces.rest;

import com.capysoft.tuevento.shared.application.translation.LanguageSummary;
import com.capysoft.tuevento.shared.application.translation.LanguageCatalog;
import com.capysoft.tuevento.modules.profile.domain.model.Profile;
import com.capysoft.tuevento.modules.profile.domain.model.ProfileTranslation;
import com.capysoft.tuevento.modules.profile.domain.repository.ProfileRepository;
import com.capysoft.tuevento.modules.profile.domain.repository.ProfileTranslationRepository;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import com.capysoft.tuevento.shared.interfaces.translation.TranslationAdminSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Administración de las traducciones de biografías de perfiles. El listado solo recorre perfiles
 * que ya tienen traducción (listar todos los perfiles sería demasiado costoso), así que "sin
 * traducir" siempre está vacío; "por revisar" incluye las traducciones en PENDING_REVIEW y un
 * filtro desconocido muestra todo.
 */
@RestController
@RequestMapping("/api/v1/admin/profile-translations")
@PreAuthorize("hasAuthority('ADMIN')")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Profile Translation Management", description = "CRUD de traducciones de perfiles de usuario")
public class ProfileTranslationController {

    private static final String TRANSLATED_BIO = "translatedBio";
    private static final String ERROR = "error";

    private final ProfileTranslationRepository profileTranslationRepository;
    private final ProfileRepository profileRepository;
    private final LanguageCatalog languageCatalog;

    @GetMapping("/{languageCode}")
    @Operation(summary = "Obtener todas las traducciones de perfiles para un idioma")
    public ResponseEntity<List<Map<String, Object>>> getProfilesWithTranslations(
            @PathVariable String languageCode,
            @RequestParam(defaultValue = TranslationAdminSupport.FILTER_ALL) String filter) {

        Optional<LanguageSummary> language = languageCatalog.findByCode(languageCode.toLowerCase());
        if (language.isEmpty()) {
            log.warn("Language not found: {}", languageCode);
            return ResponseEntity.badRequest().build();
        }
        if (TranslationAdminSupport.FILTER_NOT_TRANSLATED.equals(filter)) {
            return ResponseEntity.ok(new ArrayList<>());
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (ProfileTranslation translation : profileTranslationRepository.findByLanguage(language.get().getLanguageId())) {
            Optional<Profile> profile = profileRepository.findById(translation.getProfileId().longValue());
            if (profile.isEmpty()) {
                log.warn("Profile not found for translation: profileId={}", translation.getProfileId());
                continue;
            }
            boolean reviewFilter = TranslationAdminSupport.FILTER_NEEDS_REVIEW.equalsIgnoreCase(filter);
            boolean needsReview = needsReview(profile.get(), translation);
            if (included(filter, translation, needsReview)) {
                rows.add(row(profile.get(), translation, reviewFilter && needsReview));
            }
        }

        log.debug("Returning {} profile translations for language {}", rows.size(), languageCode);
        return ResponseEntity.ok(rows);
    }

    @PutMapping("/{profileId}/language/{languageId}")
    @Operation(summary = "Actualizar o crear traducción de un perfil")
    public ResponseEntity<Map<String, Object>> updateProfileTranslation(
            @PathVariable Long profileId,
            @PathVariable Integer languageId,
            @RequestBody Map<String, String> request) {

        if (profileRepository.findById(profileId).isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(ERROR, "Profile not found with ID: " + profileId));
        }
        Optional<LanguageSummary> language = languageCatalog.findById(languageId);
        if (language.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(ERROR, "Language not found with ID: " + languageId));
        }
        if ("es".equalsIgnoreCase(language.get().getCode())) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR, "Cannot create translation for source language (Spanish)"));
        }
        String translatedBio = request.get(TRANSLATED_BIO);
        if (translatedBio == null || translatedBio.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(ERROR, "Translated bio is required"));
        }

        ProfileTranslation translation = profileTranslationRepository.findByProfileAndLanguage(profileId, languageId)
                .orElseGet(() -> {
                    ProfileTranslation created = new ProfileTranslation();
                    created.setProfileId(profileId.intValue());
                    created.setLanguageId(languageId);
                    return created;
                });
        translation.setTranslatedBio(translatedBio.trim());
        translation.setSource(TranslationAdminSupport.sourceOf(request));
        translation.setStatus(TranslationAdminSupport.statusOf(request));
        ProfileTranslation saved = profileTranslationRepository.save(translation);

        Map<String, Object> response = new HashMap<>();
        response.put("translationId", saved.getTranslationId());
        response.put("profileId", saved.getProfileId());
        response.put("languageId", saved.getLanguageId());
        response.put(TRANSLATED_BIO, saved.getTranslatedBio());
        response.put("source", saved.getSource());
        response.put("status", saved.getStatus());
        log.info("Saved profile translation for profile {} in language {}", profileId, languageId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{profileId}/language/{languageId}")
    @Operation(summary = "Eliminar traducción de un perfil")
    public ResponseEntity<Void> deleteProfileTranslation(
            @PathVariable Long profileId,
            @PathVariable Integer languageId) {

        Optional<ProfileTranslation> translation =
                profileTranslationRepository.findByProfileAndLanguage(profileId, languageId);
        if (translation.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        profileTranslationRepository.delete(translation.get());
        log.info("Deleted profile translation for profile {} in language {}", profileId, languageId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Pendiente de revisión: traducción vacía, igual al original (sin distinguir mayúsculas ni
     * espacios) o marcada PENDING_REVIEW.
     */
    private static boolean needsReview(Profile profile, ProfileTranslation translation) {
        String translated = translation.getTranslatedBio();
        if (translated == null || translated.isBlank()) {
            return true;
        }
        String original = profile.getBio();
        return (original != null && original.trim().equalsIgnoreCase(translated.trim()))
                || translation.getStatus() == TranslationStatus.PENDING_REVIEW;
    }

    private static boolean included(String filter, ProfileTranslation translation, boolean needsReview) {
        return switch (filter.toLowerCase()) {
            case TranslationAdminSupport.FILTER_TRANSLATED ->
                    translation.getTranslatedBio() != null && !translation.getTranslatedBio().isBlank();
            case TranslationAdminSupport.FILTER_NEEDS_REVIEW -> needsReview;
            default -> true; // "all" y filtros desconocidos
        };
    }

    private static Map<String, Object> row(Profile profile, ProfileTranslation translation, boolean needsReview) {
        Map<String, Object> row = new HashMap<>();
        row.put("profileId", translation.getProfileId().longValue());
        row.put("userId", profile.getUserId());
        row.put("fullName", profile.getFullName());
        row.put("bio", profile.getBio());
        row.put("translationId", translation.getTranslationId());
        row.put(TRANSLATED_BIO, translation.getTranslatedBio());
        row.put("translationStatus", translation.getStatus());
        row.put("translationSource", translation.getSource());
        row.put("needsReview", needsReview);
        return row;
    }
}
