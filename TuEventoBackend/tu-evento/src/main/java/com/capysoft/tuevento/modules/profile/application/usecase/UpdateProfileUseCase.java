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
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import com.capysoft.tuevento.shared.domain.valueobject.ValidationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class UpdateProfileUseCase implements UpdateProfilePort {

    private static final int    NAME_CHANGE_COOLDOWN_DAYS = 14;
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ProfileRepository          profileRepository;
    private final ProfileLogRepository       profileLogRepository;
    private final CityRepository             cityRepository;
    private final ApplicationEventPublisher  eventPublisher;

    @Override
    @Transactional
    public ProfileResponse update(Long profileId, UpdateProfileRequest request) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new NotFoundException("PROFILE_NOT_FOUND",
                        "Profile not found with id: " + profileId));

        if (request.getFullName() != null) {
            ValidationUtils.validateFullName(request.getFullName());

            boolean nameActuallyChanging = !request.getFullName().equals(profile.getFullName());
            if (nameActuallyChanging) {
                enforceNameChangeCooldown(profile);
                log(profile.getProfileId(), "FULL_NAME_CHANGED",
                        profile.getFullName(), request.getFullName());
                profile.setFullName(request.getFullName());
                profile.setNameChangedAt(LocalDateTime.now());
            }
        }

        if (request.getBio() != null) {
            ValidationUtils.validateBio(request.getBio());
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

        return toResponse(saved);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Throws {@link BusinessException} if the 14-day cooldown has not elapsed.
     * Only called when the new fullName differs from the stored one.
     * Null nameChangedAt means the user has never changed their name → allowed.
     */
    private void enforceNameChangeCooldown(Profile profile) {
        if (profile.getNameChangedAt() == null) return;

        LocalDateTime nextAllowed = profile.getNameChangedAt()
                .plusDays(NAME_CHANGE_COOLDOWN_DAYS);

        if (LocalDateTime.now().isBefore(nextAllowed)) {
            throw new BusinessException(
                    "NAME_CHANGE_TOO_SOON",
                    "Podrás cambiar tu nombre de nuevo el "
                            + nextAllowed.toLocalDate().format(DATE_FMT) + ".");
        }
    }

    private ProfileResponse toResponse(Profile p) {
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
                .nameChangedAt(p.getNameChangedAt())
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
