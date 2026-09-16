package com.capysoft.tuevento.modules.security.application.usecase;

import com.capysoft.tuevento.modules.profile.infrastructure.persistence.repository.ProfileJpaRepository;
import com.capysoft.tuevento.modules.security.application.dto.response.OrganizerRequestResponse;
import com.capysoft.tuevento.modules.security.application.port.in.GetOrganizerRequestsPort;
import com.capysoft.tuevento.modules.security.domain.repository.OrganizerPetitionRepository;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.repository.LoginCredentialsJpaRepository;
import com.capysoft.tuevento.modules.storage.application.port.in.GeneratePublicUrlPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetOrganizerRequestsUseCase implements GetOrganizerRequestsPort {

    private final OrganizerPetitionRepository organizerPetitionRepository;
    private final LoginCredentialsJpaRepository loginCredentialsRepository;
    private final ProfileJpaRepository profileRepository;
    private final GeneratePublicUrlPort generatePublicUrlPort;

    @Override
    @Transactional
    public List<OrganizerRequestResponse> getPendingRequests() {
        return organizerPetitionRepository.findByStatus("PENDING").stream()
                .map(petition -> {
                    Integer userId = petition.getUser().getUserId();

                    String email = loginCredentialsRepository.findByUserUserId(userId)
                            .map(credentials -> credentials.getEmail())
                            .orElse(null);

                    String fullName       = null;
                    String profilePicture = null;

                    var profileOpt = profileRepository.findByUserId(userId);
                    if (profileOpt.isPresent()) {
                        var profile = profileOpt.get();
                        fullName = profile.getFullName();

                        Integer avatarFileId = profile.getStoredFileId();
                        // Generate a presigned URL for every avatar — default or custom.
                        // Every user is assigned an avatar when their profile is created
                        // (ProfileDataInitializer ensures the default image is always present),
                        // so avatarFileId should never be null in practice.
                        // The null guard here is a safety net for edge cases only.
                        if (avatarFileId != null) {
                            try {
                                profilePicture = generatePublicUrlPort.generate(avatarFileId).getPublicUrl();
                            } catch (Exception ex) {
                                // Orphaned or deleted file — degrade gracefully; frontend shows initial.
                                log.warn("Could not generate presigned URL for storedFileId={} (userId={}): {}",
                                        avatarFileId, userId, ex.getMessage());
                            }
                        }
                    }

                    return OrganizerRequestResponse.builder()
                            .organizerPetitionId(petition.getOrganizerPetitionId())
                            .userId(userId)
                            .fullName(fullName)
                            .email(email)
                            .documentType(petition.getDocumentType())
                            .status(petition.getStatus())
                            .applicationDate(petition.getApplicationDate())
                            .storedFileId(petition.getStoredFileId())
                            .profilePicture(profilePicture)
                            .build();
                })
                .toList();
    }
}
