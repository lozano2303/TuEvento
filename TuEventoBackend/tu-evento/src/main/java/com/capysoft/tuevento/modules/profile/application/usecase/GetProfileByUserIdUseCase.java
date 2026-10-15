package com.capysoft.tuevento.modules.profile.application.usecase;

import com.capysoft.tuevento.modules.language.application.service.TranslationService;
import com.capysoft.tuevento.modules.language.domain.model.ProfileTranslation;
import com.capysoft.tuevento.modules.language.domain.repository.LanguageRepository;
import com.capysoft.tuevento.modules.language.domain.repository.ProfileTranslationRepository;
import com.capysoft.tuevento.modules.language.domain.repository.TranslationJobRepository;
import com.capysoft.tuevento.modules.profile.application.dto.response.ProfileResponse;
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
    private final LanguageRepository languageRepository;
    private final ProfileTranslationRepository profileTranslationRepository;
    private final TranslationJobRepository translationJobRepository;
    private final TranslationService translationService;

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
                                // Translation doesn't exist - check job status before creating
                                log.info("Translation not found for profile {} language {} - checking job status",
                                        p.getProfileId(), languageCode);
                                
                                // Check if job already exists
                                var existingJob = translationJobRepository.findByEntityAndTargetLanguage(
                                        "profile", p.getProfileId().intValue(), language.get().getLanguageId());
                                
                                if (existingJob.isEmpty()) {
                                    // Create translation job
                                    translationService.requestTranslation("profile", p.getProfileId(), "es");
                                    translationGenerating = true;
                                    log.info("Lazy loading translation job created for profile {} -> {}", 
                                            p.getProfileId(), languageCode);
                                } else {
                                    // Job exists - check its status
                                    var jobStatus = existingJob.get().getStatus();
                                    if (jobStatus.name().equals("PENDING") || jobStatus.name().equals("PROCESSING")) {
                                        translationGenerating = true;
                                        log.info("Translation job in progress ({}) for profile {} -> {}", 
                                                jobStatus, p.getProfileId(), languageCode);
                                    } else if (jobStatus.name().equals("COMPLETED")) {
                                        // Job completed but no PUBLISHED translation found - this shouldn't happen
                                        // but let's handle it gracefully by not showing generating indicator
                                        log.warn("Translation job COMPLETED but no PUBLISHED translation found for profile {} -> {}", 
                                                p.getProfileId(), languageCode);
                                    } else if (jobStatus.name().equals("FAILED")) {
                                        // Job failed - could retry or show error, for now don't show generating
                                        log.warn("Translation job FAILED for profile {} -> {}", 
                                                p.getProfileId(), languageCode);
                                    }
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

        return ProfileResponse.builder()
                .profileId(p.getProfileId())
                .userId(p.getUserId())
                .cityId(p.getCity() != null ? p.getCity().getCityId() : null)
                .cityName(p.getCity() != null ? p.getCity().getName() : null)
                .departmentName(p.getCity() != null && p.getCity().getDepartment() != null
                        ? p.getCity().getDepartment().getName() : null)
                .storedFileId(p.getStoredFileId())
                .fullName(p.getFullName())
                .bio(p.getBio())
                .preferredLanguageId(p.getPreferredLanguageId())
                .nameChangedAt(p.getNameChangedAt())
                // Translation fields
                .translatedBio(translatedBio)
                .languageCode(actualLanguageCode)
                .translationGenerating(translationGenerating)
                .build();
    }
}
