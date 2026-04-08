package com.capysoft.tuevento.modules.profile.application.usecase;

import com.capysoft.tuevento.modules.profile.application.dto.request.CreateProfileRequest;
import com.capysoft.tuevento.modules.profile.application.dto.response.ProfileResponse;
import com.capysoft.tuevento.modules.profile.application.port.in.CreateProfilePort;
import com.capysoft.tuevento.modules.profile.domain.event.ProfileCreatedEvent;
import com.capysoft.tuevento.modules.profile.domain.exception.ProfileAlreadyExistsException;
import com.capysoft.tuevento.modules.profile.domain.model.Profile;
import com.capysoft.tuevento.modules.profile.domain.model.ProfileLog;
import com.capysoft.tuevento.modules.profile.domain.repository.ProfileLogRepository;
import com.capysoft.tuevento.modules.profile.domain.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CreateProfileUseCase implements CreateProfilePort {

    private final ProfileRepository     profileRepository;
    private final ProfileLogRepository  profileLogRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.profile.default-avatar-stored-file-id:#{null}}")
    private Integer defaultAvatarStoredFileId;

    @Override
    @Transactional
    public ProfileResponse create(CreateProfileRequest request) {
        if (profileRepository.existsByUserId(request.getUserId())) {
            throw new ProfileAlreadyExistsException(request.getUserId());
        }

        Profile profile = profileRepository.save(Profile.builder()
                .userId(request.getUserId())
                .fullName(request.getFullName())
                .storedFileId(defaultAvatarStoredFileId)
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
                .build();
    }
}
