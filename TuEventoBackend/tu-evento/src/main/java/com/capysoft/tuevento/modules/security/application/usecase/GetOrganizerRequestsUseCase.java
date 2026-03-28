package com.capysoft.tuevento.modules.security.application.usecase;

import com.capysoft.tuevento.modules.security.application.dto.response.OrganizerRequestResponse;
import com.capysoft.tuevento.modules.security.application.port.in.GetOrganizerRequestsPort;
import com.capysoft.tuevento.modules.security.domain.repository.OrganizerPetitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetOrganizerRequestsUseCase implements GetOrganizerRequestsPort {

    private final OrganizerPetitionRepository organizerPetitionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<OrganizerRequestResponse> getPendingRequests() {
        return organizerPetitionRepository.findByStatus("PENDING").stream()
                .map(petition -> OrganizerRequestResponse.builder()
                        .organizerPetitionId(petition.getOrganizerPetitionId())
                        .userId(petition.getUser().getUserId())
                        .alias(petition.getUser().getAlias())
                        .status(petition.getStatus())
                        .applicationDate(petition.getApplicationDate())
                        .build())
                .toList();
    }
}
