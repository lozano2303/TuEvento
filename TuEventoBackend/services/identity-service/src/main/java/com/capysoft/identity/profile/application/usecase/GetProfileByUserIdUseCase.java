package com.capysoft.identity.profile.application.usecase;

import com.capysoft.identity.profile.application.dto.response.ProfileResponse;
import com.capysoft.identity.profile.application.mapper.ProfileResponseAssembler;
import com.capysoft.identity.profile.application.port.in.GetProfileByUserIdPort;
import com.capysoft.identity.profile.application.port.out.LanguageDirectoryPort;
import com.capysoft.identity.profile.application.port.out.TranslationJobsPort;
import com.capysoft.identity.profile.domain.repository.ProfileTranslationRepository;
import com.capysoft.identity.profile.domain.model.Profile;
import com.capysoft.identity.profile.domain.repository.ProfileRepository;
import com.capysoft.common.domain.exception.NotFoundException;
import com.capysoft.identity.shared.domain.valueobject.TranslationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetProfileByUserIdUseCase implements GetProfileByUserIdPort {

    private final ProfileRepository profileRepository;
    private final ProfileResponseAssembler responseAssembler;
    private final LanguageDirectoryPort languageDirectoryPort;
    private final ProfileTranslationRepository profileTranslationRepository;
    private final TranslationJobsPort translationJobsPort;

    @Override
    public ProfileResponse getByUserId(Integer userId) {
        return getByUserId(userId, null);
    }

    @Override
    public ProfileResponse getByUserId(Integer userId, String languageCode) {
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("PROFILE_NOT_FOUND",
                        "Profile not found for user id: " + userId));
        
        return toResponse(profile, languageCode);
    }

    private ProfileResponse toResponse(Profile p, String languageCode) {
        String translatedBio = null;
        String actualLanguageCode = null;
        Boolean translationGenerating = false;

        if (languageCode != null && !languageCode.isBlank()) {
            try {
                // IMPORTANT: Don't translate if requested language is the same as source language (Spanish)
                // Profiles are created in Spanish by default
                if ("es".equalsIgnoreCase(languageCode)) {
                    // Return original content without translation for Spanish
                    log.debug("Requested language is Spanish (source language) - no translation needed for profile {}", 
                            p.getProfileId());
                } else {
                    var languageId = languageDirectoryPort.findIdByCode(languageCode);
                    if (languageId.isPresent()) {
                        var translation = profileTranslationRepository.findByProfileAndLanguage(
                                p.getProfileId(), languageId.get());
                        
                        if (translation.isPresent() && translation.get().getStatus() == TranslationStatus.PUBLISHED) {
                            translatedBio = translation.get().getTranslatedBio();
                            actualLanguageCode = languageCode;
                        } else {
                            // Only trigger translation if there's content to translate
                            boolean hasBio = p.getBio() != null && !p.getBio().trim().isEmpty();
                            
                            if (hasBio) {
                                // Translation doesn't exist - check job status before creating
                                log.info("Translation not found for profile {} language {} - checking job status",
                                        p.getProfileId(), languageCode);
                                
                                switch (translationJobsPort.stateOf(p.getProfileId(), languageId.get())) {
                                    case NONE -> {
                                        translationJobsPort.request(p.getProfileId(), "es");
                                        translationGenerating = true;
                                        log.info("Lazy loading translation job created for profile {} -> {}",
                                                p.getProfileId(), languageCode);
                                    }
                                    case IN_PROGRESS -> {
                                        translationGenerating = true;
                                        log.info("Translation job in progress for profile {} -> {}",
                                                p.getProfileId(), languageCode);
                                    }
                                    // Completado sin traducción PUBLISHED no debería pasar: no se muestra el indicador
                                    case COMPLETED -> log.warn("Translation job COMPLETED but no PUBLISHED translation found for profile {} -> {}",
                                            p.getProfileId(), languageCode);
                                    case FAILED -> log.warn("Translation job FAILED for profile {} -> {}",
                                            p.getProfileId(), languageCode);
                                }
                            } else {
                                log.debug("Profile {} has no bio - skipping translation", p.getProfileId());
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                log.warn("Could not resolve translation for profile {} language {}: {}",
                        p.getProfileId(), languageCode, ex.getMessage());
            }
        }

        return responseAssembler.builder(p)
                .preferredLanguageId(p.getPreferredLanguageId())
                .nameChangedAt(p.getNameChangedAt())
                // Translation fields
                .translatedBio(translatedBio)
                .languageCode(actualLanguageCode)
                .translationGenerating(translationGenerating)
                .build();
    }
}
