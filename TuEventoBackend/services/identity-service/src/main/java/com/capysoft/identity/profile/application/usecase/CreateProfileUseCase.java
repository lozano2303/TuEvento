package com.capysoft.identity.profile.application.usecase;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.capysoft.identity.profile.application.dto.request.CreateProfileRequest;
import com.capysoft.identity.profile.application.dto.response.ProfileResponse;
import com.capysoft.identity.profile.application.port.in.CreateProfilePort;
import com.capysoft.identity.profile.domain.event.ProfileCreatedEvent;
import com.capysoft.identity.profile.domain.exception.ProfileAlreadyExistsException;
import com.capysoft.identity.profile.domain.model.Profile;
import com.capysoft.identity.profile.domain.model.ProfileLog;
import com.capysoft.identity.profile.domain.repository.ProfileLogRepository;
import com.capysoft.identity.profile.domain.repository.ProfileRepository;
import com.capysoft.identity.shared.domain.valueobject.ValidationUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateProfileUseCase implements CreateProfilePort {

    private final ProfileRepository     profileRepository;
    private final ProfileLogRepository  profileLogRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.profile.default-avatar-stored-file-id:#{null}}")
    private Integer defaultAvatarStoredFileId;
    
    @Value("${app.profile.default-language-id:1}")
    private Long defaultLanguageId; // 1 = Español

    @Override
    @Transactional
    public ProfileResponse create(CreateProfileRequest request) {
        if (profileRepository.existsByUserId(request.getUserId())) {
            throw new ProfileAlreadyExistsException(request.getUserId());
        }

        // Validate fullName before persisting — makes this use case self-defending
        // against direct REST API calls that bypass caller-level validation.
        // Internal callers (OauthLoginUseCase, DataInitializer) already pre-validate,
        // so this is a safety net, not a duplicate check.
        ValidationUtils.validateFullName(request.getFullName());

        Profile profile = profileRepository.save(Profile.builder()
                .userId(request.getUserId())
                .fullName(request.getFullName())
                .storedFileId(request.isStoredFileIdSet()
                        ? request.getStoredFileId()
                        : defaultAvatarStoredFileId)
                .preferredLanguageId(request.getPreferredLanguageId() != null 
                        ? request.getPreferredLanguageId().longValue() 
                        : defaultLanguageId)
                .build());

        profileLogRepository.save(ProfileLog.builder()
                .profileId(profile.getProfileId())
                .action("CREATED")
                .newValue(request.getFullName())
                .occurredAt(LocalDateTime.now())
                .build());

        eventPublisher.publishEvent(ProfileCreatedEvent.builder()
                .profileId(profile.getProfileId())
                .userId(profile.getUserId())
                .occurredAt(LocalDateTime.now())
                .build());

        return ProfileResponse.builder()
                .profileId(profile.getProfileId())
                .userId(profile.getUserId())
                .storedFileId(profile.getStoredFileId())
                .fullName(profile.getFullName())
                .bio(profile.getBio())
                .preferredLanguageId(profile.getPreferredLanguageId())
                .build();
    }
}
