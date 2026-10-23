package com.capysoft.tuevento.modules.profile.infrastructure.translation;

import com.capysoft.tuevento.modules.language.application.port.out.TranslatableContentHandler;
import com.capysoft.tuevento.modules.language.domain.model.ProfileTranslation;
import com.capysoft.tuevento.modules.language.domain.repository.LanguageRepository;
import com.capysoft.tuevento.modules.language.domain.repository.ProfileTranslationRepository;
import com.capysoft.tuevento.modules.profile.domain.model.Profile;
import com.capysoft.tuevento.modules.profile.domain.repository.ProfileRepository;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationSource;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Handler para la traducción de contenido de perfiles.
 * Maneja la carga de textos originales y el guardado de traducciones para perfiles.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProfileTranslatableContentHandler implements TranslatableContentHandler {

    private final ProfileRepository profileRepository;
    private final ProfileTranslationRepository profileTranslationRepository;
    private final LanguageRepository languageRepository;

    @Override
    public String entityType() {
        return "profile";
    }

    @Override
    public Map<String, String> loadSourceTexts(Long profileId) {
        log.info("=== LOAD SOURCE TEXTS === Profile: {}", profileId);
        
        Optional<Profile> profileOpt = profileRepository.findById(profileId);
        if (profileOpt.isEmpty()) {
            log.warn("Profile not found: {}", profileId);
            return Map.of();
        }

        Profile profile = profileOpt.get();
        Map<String, String> sourceTexts = new HashMap<>();
        
        // Solo incluir biografía si tiene contenido
        if (profile.getBio() != null && !profile.getBio().trim().isEmpty()) {
            sourceTexts.put("bio", profile.getBio());
            log.info("Added bio text: '{}'", profile.getBio());
        }
        
        log.info("=== SOURCE TEXTS LOADED === Profile: {}, Fields: {}", profileId, sourceTexts.keySet());
        return sourceTexts;
    }

    @Override
    public void saveTranslation(Long entityId, String languageCode, Map<String, String> translatedTexts, 
                               TranslationSource source, TranslationStatus status) {
        log.info("=== SAVE TRANSLATION START === Profile: {}, Lang: {}, Texts: {}", entityId, languageCode, translatedTexts);
        
        // 1. Buscar el idioma por código
        var languageOpt = languageRepository.findByCode(languageCode);
        if (languageOpt.isEmpty()) {
            log.warn("Language not found: {}", languageCode);
            return;
        }
        
        Integer languageId = languageOpt.get().getLanguageId();
        log.info("Found language: {} -> ID: {}", languageCode, languageId);
        
        // 2. Buscar traducción existente
        Optional<ProfileTranslation> existingOpt = profileTranslationRepository
                .findByProfileAndLanguage(entityId, languageId);
        
        ProfileTranslation translation;
        if (existingOpt.isPresent()) {
            translation = existingOpt.get();
            log.info("Found existing translation ID: {}", translation.getTranslationId());
        } else {
            translation = new ProfileTranslation();
            translation.setProfileId(entityId.intValue());
            translation.setLanguageId(languageId);
            log.info("Creating new translation for Profile: {}, Language: {}", entityId, languageId);
        }
        
        // 3. Procesar cada campo traducido
        if (translatedTexts.containsKey("bio")) {
            String translatedBio = translatedTexts.get("bio");
            log.info("Processing bio: '{}'", translatedBio);
            
            if (translatedBio != null && !translatedBio.trim().isEmpty()) {
                translation.setTranslatedBio(translatedBio);
                log.info("Set translated bio");
            }
        }
        
        // 4. Establecer metadatos
        translation.setSource(source);
        translation.setStatus(status);
        
        // 5. Guardar
        profileTranslationRepository.save(translation);
        log.info("=== TRANSLATION SAVED === Profile: {}, Lang: {}", entityId, languageCode);
    }

    @Override
    public Optional<TranslationStatus> findTranslationStatus(Long entityId, String languageCode) {
        var languageOpt = languageRepository.findByCode(languageCode);
        if (languageOpt.isEmpty()) {
            return Optional.empty();
        }
        
        return profileTranslationRepository
                .findByProfileAndLanguage(entityId, languageOpt.get().getLanguageId())
                .map(ProfileTranslation::getStatus);
    }
}