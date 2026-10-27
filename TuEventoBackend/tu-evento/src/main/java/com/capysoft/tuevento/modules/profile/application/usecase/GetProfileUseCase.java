package com.capysoft.tuevento.modules.profile.application.usecase;

import com.capysoft.tuevento.modules.profile.application.dto.response.ProfileResponse;
import com.capysoft.tuevento.modules.profile.application.port.in.GetProfilePort;
import com.capysoft.tuevento.modules.profile.application.mapper.ProfileResponseAssembler;
import com.capysoft.tuevento.modules.profile.domain.repository.ProfileRepository;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetProfileUseCase implements GetProfilePort {

    private final ProfileRepository profileRepository;
    private final ProfileResponseAssembler responseAssembler;

    @Override
    public ProfileResponse getById(Long profileId) {
        return profileRepository.findById(profileId)
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("PROFILE_NOT_FOUND",
                        "Profile not found with id: " + profileId));
    }

    private ProfileResponse toResponse(com.capysoft.tuevento.modules.profile.domain.model.Profile p) {
        return responseAssembler.builder(p)
                .build();
    }
}
