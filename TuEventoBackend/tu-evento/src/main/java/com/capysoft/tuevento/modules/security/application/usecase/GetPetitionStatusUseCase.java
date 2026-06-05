package com.capysoft.tuevento.modules.security.application.usecase;

import com.capysoft.tuevento.modules.security.application.dto.response.RequestOrganizerResponse;
import com.capysoft.tuevento.modules.security.application.port.in.GetPetitionStatusPort;
import com.capysoft.tuevento.modules.security.domain.repository.OrganizerPetitionRepository;
import com.capysoft.tuevento.modules.security.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetPetitionStatusUseCase implements GetPetitionStatusPort {

    private final OrganizerPetitionRepository organizerPetitionRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public RequestOrganizerResponse getByUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        String alias = authentication.getName();
        var user = userRepository.findByAlias(alias).orElse(null);
        if (user == null) {
            return null;
        }

        return organizerPetitionRepository.findPendingByUserId(user.getUserId())
                .map(petition -> RequestOrganizerResponse.builder()
                        .organizerPetitionId(petition.getOrganizerPetitionId())
                        .status(petition.getStatus())
                        .applicationDate(petition.getApplicationDate())
                        .storedFileId(petition.getStoredFileId())
                        .build())
                .orElse(null);
    }
}