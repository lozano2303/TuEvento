package com.capysoft.tuevento.modules.security.application.usecase;

import com.capysoft.tuevento.modules.profile.infrastructure.persistence.repository.ProfileJpaRepository;
import com.capysoft.tuevento.modules.security.application.dto.response.OrganizerUserResponse;
import com.capysoft.tuevento.modules.security.application.port.in.GetOrganizersPort;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.repository.LoginCredentialsJpaRepository;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.repository.UserJpaRepository;
import com.capysoft.tuevento.modules.storage.application.port.in.GeneratePublicUrlPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetOrganizersUseCase implements GetOrganizersPort {

    private final UserJpaRepository             userJpaRepository;
    private final LoginCredentialsJpaRepository loginCredentialsRepository;
    private final ProfileJpaRepository          profileRepository;
    private final GeneratePublicUrlPort         generatePublicUrlPort;

    @Override
    @Transactional
    public List<OrganizerUserResponse> getOrganizers() {
        return userJpaRepository.findAllByRoleCode("ORGANIZER").stream()
                .map(userEntity -> {
                    Integer userId = userEntity.getUserId();

                    // Email — lightweight scalar query
                    String email = null;
                    try {
                        email = loginCredentialsRepository.findEmailByUserId(userId).orElse(null);
                    } catch (Exception ex) {
                        log.warn("Could not load email for userId={}: {}", userId, ex.getMessage());
                    }

                    // Profile: fullName + presigned avatar URL
                    String fullName       = null;
                    String profilePicture = null;
                    try {
                        var profileOpt = profileRepository.findByUserId(userId);
                        if (profileOpt.isPresent()) {
                            var profile = profileOpt.get();
                            fullName = profile.getFullName();
                            Integer avatarFileId = profile.getStoredFileId();
                            if (avatarFileId != null) {
                                try {
                                    profilePicture = generatePublicUrlPort.generate(avatarFileId).getPublicUrl();
                                } catch (Exception ex) {
                                    log.warn("Could not generate presigned URL for storedFileId={} (userId={}): {}",
                                            avatarFileId, userId, ex.getMessage());
                                }
                            }
                        }
                    } catch (Exception ex) {
                        log.warn("Could not load profile for userId={}: {}", userId, ex.getMessage());
                    }

                    // Event counts — dedicated count queries, no full entity load
                    long totalEvents     = 0L;
                    long publishedEvents = 0L;
                    try {
                        totalEvents     = userJpaRepository.countEventsByUserId((long) userId);
                        publishedEvents = userJpaRepository.countPublishedEventsByUserId((long) userId);
                    } catch (Exception ex) {
                        log.warn("Could not count events for userId={}: {}", userId, ex.getMessage());
                    }

                    return OrganizerUserResponse.builder()
                            .userId(userId)
                            .alias(userEntity.getAlias())
                            .fullName(fullName)
                            .email(email)
                            .profilePicture(profilePicture)
                            .totalEvents(totalEvents)
                            .publishedEvents(publishedEvents)
                            .memberSince(userEntity.getCreatedAt())
                            .build();
                })
                .toList();
    }
}
