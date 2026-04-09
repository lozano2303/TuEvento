package com.capysoft.tuevento.modules.profile.application.usecase;

import com.capysoft.tuevento.modules.geolocation.domain.model.City;
import com.capysoft.tuevento.modules.geolocation.domain.repository.CityRepository;
import com.capysoft.tuevento.modules.profile.application.dto.request.UpdateProfileRequest;
import com.capysoft.tuevento.modules.profile.application.dto.response.ProfileResponse;
import com.capysoft.tuevento.modules.profile.application.port.in.UpdateProfilePort;
import com.capysoft.tuevento.modules.profile.domain.event.ProfileUpdatedEvent;
import com.capysoft.tuevento.modules.profile.domain.model.Profile;
import com.capysoft.tuevento.modules.profile.domain.model.ProfileLog;
import com.capysoft.tuevento.modules.profile.domain.repository.ProfileLogRepository;
import com.capysoft.tuevento.modules.profile.domain.repository.ProfileRepository;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UpdateProfileUseCase implements UpdateProfilePort {

    private final ProfileRepository     profileRepository;
    private final ProfileLogRepository  profileLogRepository;
    private final CityRepository        cityRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public ProfileResponse update(Long profileId, UpdateProfileRequest request) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new NotFoundException("PROFILE_NOT_FOUND",
                        "Profile not found with id: " + profileId));

        if (request.getFullName() != null) {
            log(profile.getProfileId(), "FULL_NAME_CHANGED", profile.getFullName(), request.getFullName());
            profile.setFullName(request.getFullName());
        }
        if (request.getBio() != null) {
            log(profile.getProfileId(), "BIO_CHANGED", profile.getBio(), request.getBio());
            profile.setBio(request.getBio());
        }
        if (request.getStoredFileId() != null) {
            log(profile.getProfileId(), "AVATAR_CHANGED",
                    profile.getStoredFileId() != null ? profile.getStoredFileId().toString() : null,
                    request.getStoredFileId().toString());
            profile.setStoredFileId(request.getStoredFileId());
        }
        if (request.getCityId() != null) {
            City city = cityRepository.findById(request.getCityId())
                    .orElseThrow(() -> new NotFoundException("CITY_NOT_FOUND",
                            "City not found with id: " + request.getCityId()));
            log(profile.getProfileId(), "CITY_CHANGED",
                    profile.getCity() != null ? profile.getCity().getCityId().toString() : null,
                    request.getCityId().toString());
            profile.setCity(city);
        }

        Profile saved = profileRepository.save(profile);

        eventPublisher.publishEvent(ProfileUpdatedEvent.builder()
                .profileId(saved.getProfileId())
                .userId(saved.getUserId())
                .occurredAt(LocalDateTime.now())
                .build());

        return ProfileResponse.builder()
                .profileId(saved.getProfileId())
                .userId(saved.getUserId())
                .cityId(saved.getCity() != null ? saved.getCity().getCityId() : null)
                .cityName(saved.getCity() != null ? saved.getCity().getName() : null)
                .departmentName(saved.getCity() != null && saved.getCity().getDepartment() != null
                        ? saved.getCity().getDepartment().getName() : null)
                .storedFileId(saved.getStoredFileId())
                .fullName(saved.getFullName())
                .bio(saved.getBio())
                .build();
    }

    private void log(Long profileId, String action, String oldValue, String newValue) {
        profileLogRepository.save(ProfileLog.builder()
                .profileId(profileId)
                .action(action)
                .oldValue(oldValue)
                .newValue(newValue)
                .occurredAt(LocalDateTime.now())
                .build());
    }
}
