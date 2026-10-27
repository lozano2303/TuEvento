package com.capysoft.tuevento.modules.profile.application.usecase;

import com.capysoft.tuevento.shared.application.translation.TranslationRequests;
import com.capysoft.tuevento.modules.profile.domain.model.ProfileTranslation;
import com.capysoft.tuevento.modules.language.domain.repository.LanguageRepository;
import com.capysoft.tuevento.modules.profile.domain.repository.ProfileTranslationRepository;
import com.capysoft.tuevento.modules.profile.application.dto.response.ProfileResponse;
import com.capysoft.tuevento.modules.profile.application.mapper.ProfileResponseAssembler;
import com.capysoft.tuevento.modules.profile.application.port.in.GetProfileByUserIdPort;
import com.capysoft.tuevento.modules.profile.domain.model.Profile;
import com.capysoft.tuevento.modules.profile.domain.repository.ProfileRepository;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetProfileByUserIdUseCase implements GetProfileByUserIdPort {

    private final ProfileRepository profileRepository;
    private final ProfileResponseAssembler responseAssembler;
    private final LanguageRepository languageRepository;
    private final ProfileTranslationRepository profileTranslationRepository;
    private final TranslationRequests translationRequests;

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
                    var language = languageRepository.findByCode(languageCode);
                    if (language.isPresent()) {
                        var translation = profileTranslationRepository.findByProfileAndLanguage(
                                p.getProfileId(), language.get().getLanguageId());
                        
                        if (translation.isPresent() && translation.get().getStatus() == TranslationStatus.PUBLISHED) {
                            translatedBio = translation.get().getTranslatedBio();
                            actualLanguageCode = languageCode;
                        } else {
                            // Only trigger translation if there's content to translate
                            boolean hasBio = p.getBio() != null && !p.getBio().trim().isEmpty();
                            
                            if (hasBio) {
                                translationGenerating = translationRequests.ensureRequested(
                                        "profile", p.getProfileId(), language.get().getLanguageId());
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
