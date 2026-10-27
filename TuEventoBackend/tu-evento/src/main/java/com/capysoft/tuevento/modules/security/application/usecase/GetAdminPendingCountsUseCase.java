package com.capysoft.tuevento.modules.security.application.usecase;

import com.capysoft.tuevento.modules.security.application.port.out.AdminPanelSourcesPort;
import com.capysoft.tuevento.modules.security.application.dto.response.AdminPendingCountsResponse;
import com.capysoft.tuevento.modules.security.application.port.in.GetAdminPendingCountsPort;
import com.capysoft.tuevento.modules.security.domain.repository.OrganizerPetitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetAdminPendingCountsUseCase implements GetAdminPendingCountsPort {

    private static final String PENDING_STATUS = "PENDING";

    private final OrganizerPetitionRepository organizerPetitionRepository;
    private final AdminPanelSourcesPort       adminPanelSources;

    @Override
    @Transactional(readOnly = true)
    public AdminPendingCountsResponse getCounts() {
        return AdminPendingCountsResponse.builder()
                .organizerRequests(organizerPetitionRepository.findByStatus(PENDING_STATUS).size())
                .eventsInReview(adminPanelSources.eventsPendingReview())
                .build();
    }
}
