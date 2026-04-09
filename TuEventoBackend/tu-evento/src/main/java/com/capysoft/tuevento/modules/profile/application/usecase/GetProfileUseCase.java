package com.capysoft.tuevento.modules.profile.application.usecase;

import com.capysoft.tuevento.modules.profile.application.dto.response.ProfileResponse;
import com.capysoft.tuevento.modules.profile.application.port.in.GetProfilePort;
import com.capysoft.tuevento.modules.profile.domain.repository.ProfileRepository;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetProfileUseCase implements GetProfilePort {

    private final ProfileRepository profileRepository;

    @Override
    public ProfileResponse getById(Long profileId) {
        return profileRepository.findById(profileId)
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("PROFILE_NOT_FOUND",
                        "Profile not found with id: " + profileId));
    }

    private ProfileResponse toResponse(com.capysoft.tuevento.modules.profile.domain.model.Profile p) {
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
                .build();
    }
}
