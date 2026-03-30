package com.capysoft.tuevento.modules.security.application.usecase;

import com.capysoft.tuevento.modules.security.application.port.in.RejectOrganizerRequestPort;
import com.capysoft.tuevento.modules.security.domain.model.OrganizerPetition;
import com.capysoft.tuevento.modules.security.domain.repository.OrganizerPetitionRepository;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RejectOrganizerRequestUseCase implements RejectOrganizerRequestPort {

    private final OrganizerPetitionRepository organizerPetitionRepository;

    @Override
    @Transactional
    public void reject(Integer organizerPetitionId) {
        OrganizerPetition petition = organizerPetitionRepository.findById(organizerPetitionId)
                .orElseThrow(() -> new NotFoundException("PETITION_NOT_FOUND", "Organizer petition not found"));

        if (!"PENDING".equals(petition.getStatus())) {
            throw new BusinessException("PETITION_NOT_PENDING", "Only pending petitions can be rejected");
        }

        petition.setStatus("REJECTED");
        organizerPetitionRepository.save(petition);
    }
}
