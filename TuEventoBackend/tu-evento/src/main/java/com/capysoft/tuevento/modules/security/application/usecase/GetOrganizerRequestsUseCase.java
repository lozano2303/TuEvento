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

    /** storedFileId that represents the system default avatar — no presigned URL needed for it. */
    private static final int DEFAULT_AVATAR_STORED_FILE_ID = 1;

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

                    String fullName = null;
                    String profilePicture = null;

                    var profileOpt = profileRepository.findByUserId(userId);
                    if (profileOpt.isPresent()) {
                        var profile = profileOpt.get();
                        fullName = profile.getFullName();

                        Integer avatarFileId = profile.getStoredFileId();
                        // Only generate a presigned URL for custom avatars.
                        // If the file id points to the default avatar (or is absent),
                        // leave profilePicture null so the frontend shows the initial.
                        if (avatarFileId != null && avatarFileId != DEFAULT_AVATAR_STORED_FILE_ID) {
                            try {
                                profilePicture = generatePublicUrlPort.generate(avatarFileId).getPublicUrl();
                            } catch (Exception ex) {
                                // Orphaned or deleted file — degrade gracefully, show initial instead.
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
                            .documentType("Cédula")
                            .status(petition.getStatus())
                            .applicationDate(petition.getApplicationDate())
                            .storedFileId(petition.getStoredFileId())
                            .profilePicture(profilePicture)
                            .build();
                })
                .toList();
    }
}
