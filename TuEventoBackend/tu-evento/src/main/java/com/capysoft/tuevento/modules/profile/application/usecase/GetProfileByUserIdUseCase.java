package com.capysoft.tuevento.modules.profile.application.usecase;

import com.capysoft.tuevento.modules.profile.application.dto.response.ProfileResponse;
import com.capysoft.tuevento.modules.profile.application.port.in.GetProfileByUserIdPort;
import com.capysoft.tuevento.modules.profile.domain.model.Profile;
import com.capysoft.tuevento.modules.profile.domain.repository.ProfileRepository;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetProfileByUserIdUseCase implements GetProfileByUserIdPort {

    private final ProfileRepository profileRepository;

    @Override
    public ProfileResponse getByUserId(Integer userId) {
        return profileRepository.findByUserId(userId)
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("PROFILE_NOT_FOUND",
                        "Profile not found for user id: " + userId));
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
                .build();
    }
}
