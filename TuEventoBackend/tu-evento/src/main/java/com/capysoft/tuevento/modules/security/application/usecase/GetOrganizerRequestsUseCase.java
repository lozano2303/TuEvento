package com.capysoft.tuevento.modules.security.application.usecase;

import com.capysoft.tuevento.modules.profile.infrastructure.persistence.repository.ProfileJpaRepository;
import com.capysoft.tuevento.modules.security.application.dto.response.OrganizerRequestResponse;
import com.capysoft.tuevento.modules.security.application.port.in.GetOrganizerRequestsPort;
import com.capysoft.tuevento.modules.security.domain.repository.OrganizerPetitionRepository;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.repository.LoginCredentialsJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetOrganizerRequestsUseCase implements GetOrganizerRequestsPort {

    private final OrganizerPetitionRepository organizerPetitionRepository;
    private final LoginCredentialsJpaRepository loginCredentialsRepository;
    private final ProfileJpaRepository profileRepository;

    @Override
    @Transactional(readOnly = true)
    public List<OrganizerRequestResponse> getPendingRequests() {
        return organizerPetitionRepository.findByStatus("PENDING").stream()
                .map(petition -> {
                    String email = loginCredentialsRepository.findByUserUserId(petition.getUser().getUserId())
                            .map(credentials -> credentials.getEmail())
                            .orElse(null);
                    
                    String fullName = profileRepository.findByUserId(petition.getUser().getUserId())
                            .map(profile -> profile.getFullName())
                            .orElse(null);
                    
                    return OrganizerRequestResponse.builder()
                            .organizerPetitionId(petition.getOrganizerPetitionId())
                            .userId(petition.getUser().getUserId())
                            .fullName(fullName)
                            .email(email)
                            .documentType("Cédula")
                            .status(petition.getStatus())
                            .applicationDate(petition.getApplicationDate())
                             .storedFileId(petition.getStoredFileId())
                             .build();
                })
                .toList();
    }
}
