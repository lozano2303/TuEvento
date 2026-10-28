package com.capysoft.identity.profile.interfaces.rest;

import com.capysoft.identity.profile.application.port.out.LanguageDirectoryPort;
import com.capysoft.identity.profile.domain.model.Profile;
import com.capysoft.identity.profile.domain.model.ProfileTranslation;
import com.capysoft.identity.profile.domain.repository.ProfileRepository;
import com.capysoft.identity.profile.domain.repository.ProfileTranslationRepository;
import com.capysoft.identity.shared.domain.valueobject.TranslationSource;
import com.capysoft.identity.shared.domain.valueobject.TranslationStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/admin/profile-translations")
@PreAuthorize("hasAuthority('ADMIN')")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Profile Translation Management", description = "CRUD de traducciones de perfiles de usuario")
public class ProfileTranslationController {

    private final ProfileTranslationRepository profileTranslationRepository;
    private final ProfileRepository profileRepository;
    private final LanguageDirectoryPort languageDirectoryPort;

    @Operation(summary = "Obtener todas las traducciones de perfiles para un idioma",
               description = "Lista todos los perfiles con traducciones existentes para el idioma especificado")
    @GetMapping("/{languageCode}")
    public ResponseEntity<List<Map<String, Object>>> getProfilesWithTranslations(
            @Parameter(description = "Language code (en, fr, pt)", example = "en") 
            @PathVariable String languageCode,
            
            @Parameter(description = "Filter: all, needs-review, translated, not-translated", example = "all")
            @RequestParam(defaultValue = "all") String filter) {
        
        try {
            log.debug("Getting profile translations for language: {}, filter: {}", languageCode, filter);
            
            Optional<Integer> languageIdOpt = languageDirectoryPort.findIdByCode(languageCode.toLowerCase());
            if (languageIdOpt.isEmpty()) {
                log.warn("Language not found: {}", languageCode);
                return ResponseEntity.badRequest().build();
            }
            
            Integer languageId = languageIdOpt.get();
            
            List<ProfileTranslation> translations = profileTranslationRepository.findByLanguage(languageId);
            
            if ("not-translated".equals(filter)) {
                log.debug("Filter 'not-translated' returns empty list for profiles");
                return ResponseEntity.ok(new ArrayList<>());
            }
            
            List<Map<String, Object>> result = new ArrayList<>();
            
            for (ProfileTranslation translation : translations) {
                Integer profileId = translation.getProfileId();
                Optional<Profile> profileOpt = profileRepository.findById(profileId.longValue());
                
                if (profileOpt.isEmpty()) {
                    log.warn("Profile not found for translation: profileId={}", profileId);
                    continue;
                }
                
                Profile profile = profileOpt.get();
                
                boolean includeInResult = false;
                boolean needsReview = false;
                
                switch (filter.toLowerCase()) {
                    case "all":
                        includeInResult = true;
                        break;
                        
                    case "translated":
                        includeInResult = translation.getTranslatedBio() != null && 
                                         !translation.getTranslatedBio().trim().isEmpty();
                        break;
                        
                    case "needs-review":
                        String original = profile.getBio();
                        String translated = translation.getTranslatedBio();
                        
                        if (translated == null || translated.trim().isEmpty()) {
                            includeInResult = true;
                            needsReview = true;
                        } else if (original != null && original.equals(translated)) {
                            includeInResult = true;
                            needsReview = true;
                        } else if (original != null && original.toLowerCase().trim().equals(translated.toLowerCase().trim())) {
                            includeInResult = true;
                            needsReview = true;
                        } else if (translation.getStatus() == TranslationStatus.PENDING_REVIEW) {
                            includeInResult = true;
                            needsReview = true;
                        }
                        break;
                        
                    default:
                        includeInResult = true;
                        break;
                }
                
                if (!includeInResult) {
                    continue;
                }
                
                Map<String, Object> profileData = new HashMap<>();
                profileData.put("profileId", profileId.longValue());
                profileData.put("userId", profile.getUserId());
                profileData.put("fullName", profile.getFullName());
                profileData.put("bio", profile.getBio());
                profileData.put("translationId", translation.getTranslationId());
                profileData.put("translatedBio", translation.getTranslatedBio());
                profileData.put("translationStatus", translation.getStatus());
                profileData.put("translationSource", translation.getSource());
                profileData.put("needsReview", needsReview);
                
                result.add(profileData);
            }
            
            log.debug("Returning {} profile translations for language {}", result.size(), languageCode);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error getting profile translations for language: {}", languageCode, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Actualizar o crear traducción de un perfil",
               description = "Crea o actualiza la traducción de un perfil de usuario")
    @PutMapping("/{profileId}/language/{languageId}")
    public ResponseEntity<Map<String, Object>> updateProfileTranslation(
            @Parameter(description = "Profile ID", example = "1") 
            @PathVariable Long profileId,
            
            @Parameter(description = "Language ID (2=en, 3=fr, 4=pt)", example = "2") 
            @PathVariable Integer languageId,
            
            @RequestBody Map<String, Object> request) {
        
        try {
            log.debug("Updating profile translation: profileId={}, languageId={}", profileId, languageId);
            
            Optional<Profile> profileOpt = profileRepository.findById(profileId);
            if (profileOpt.isEmpty()) {
                log.warn("Profile not found: {}", profileId);
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Profile not found with ID: " + profileId));
            }
            
            Optional<String> languageCodeOpt = languageDirectoryPort.findCodeById(languageId);
            if (languageCodeOpt.isEmpty()) {
                log.warn("Language not found: {}", languageId);
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Language not found with ID: " + languageId));
            }
            
            if ("es".equalsIgnoreCase(languageCodeOpt.get())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Cannot create translation for source language (Spanish)"));
            }
            
            String translatedBio = (String) request.get("translatedBio");
            if (translatedBio == null || translatedBio.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Translated bio is required"));
            }
            
            Optional<ProfileTranslation> existingOpt = profileTranslationRepository
                    .findByProfileAndLanguage(profileId, languageId);
            
            ProfileTranslation translation;
            if (existingOpt.isPresent()) {
                translation = existingOpt.get();
                translation.setTranslatedBio(translatedBio.trim());
            } else {
                translation = new ProfileTranslation();
                translation.setProfileId(profileId.intValue());
                translation.setLanguageId(languageId);
                translation.setTranslatedBio(translatedBio.trim());
            }
            
            if (request.containsKey("source")) {
                try {
                    translation.setSource(TranslationSource.valueOf((String) request.get("source")));
                } catch (IllegalArgumentException e) {
                    translation.setSource(TranslationSource.MANUAL);
                }
            } else {
                translation.setSource(TranslationSource.MANUAL);
            }
            
            if (request.containsKey("status")) {
                try {
                    translation.setStatus(TranslationStatus.valueOf((String) request.get("status")));
                } catch (IllegalArgumentException e) {
                    translation.setStatus(TranslationStatus.PUBLISHED);
                }
            } else {
                translation.setStatus(TranslationStatus.PUBLISHED);
            }
            
            ProfileTranslation saved = profileTranslationRepository.save(translation);
            
            Map<String, Object> response = new HashMap<>();
            response.put("translationId", saved.getTranslationId());
            response.put("profileId", saved.getProfileId());
            response.put("languageId", saved.getLanguageId());
            response.put("translatedBio", saved.getTranslatedBio());
            response.put("source", saved.getSource());
            response.put("status", saved.getStatus());
            
            log.info("Profile translation {} for profile {} in language {}", 
                    saved.getTranslationId() == null ? "created" : "updated", 
                    profileId, languageId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error updating profile translation: profileId={}, languageId={}", profileId, languageId, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error updating translation: " + e.getMessage()));
        }
    }

    @Operation(summary = "Eliminar traducción de un perfil",
               description = "Elimina la traducción de un perfil en un idioma específico")
    @DeleteMapping("/{profileId}/language/{languageId}")
    public ResponseEntity<Void> deleteProfileTranslation(
            @Parameter(description = "Profile ID", example = "1") 
            @PathVariable Long profileId,
            
            @Parameter(description = "Language ID (2=en, 3=fr, 4=pt)", example = "2") 
            @PathVariable Integer languageId) {
        
        try {
            log.debug("Deleting profile translation: profileId={}, languageId={}", profileId, languageId);
            
            Optional<ProfileTranslation> translationOpt = profileTranslationRepository
                    .findByProfileAndLanguage(profileId, languageId);
            
            if (translationOpt.isEmpty()) {
                log.warn("Profile translation not found: profileId={}, languageId={}", profileId, languageId);
                return ResponseEntity.notFound().build();
            }
            
            profileTranslationRepository.delete(translationOpt.get());
            
            log.info("Deleted profile translation for profile {} in language {}", profileId, languageId);
            return ResponseEntity.noContent().build();
            
        } catch (Exception e) {
            log.error("Error deleting profile translation: profileId={}, languageId={}", profileId, languageId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
