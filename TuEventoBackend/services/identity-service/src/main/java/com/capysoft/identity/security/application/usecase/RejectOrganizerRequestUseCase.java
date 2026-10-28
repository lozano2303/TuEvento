package com.capysoft.identity.security.application.usecase;
import com.capysoft.identity.security.application.port.in.RejectOrganizerRequestPort;
import com.capysoft.identity.security.domain.event.OrganizerRejectedEvent;
import com.capysoft.identity.security.domain.model.OrganizerPetition;
import com.capysoft.identity.security.domain.repository.OrganizerPetitionRepository;
import com.capysoft.common.domain.exception.BusinessException;
import com.capysoft.common.domain.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
@Service
@RequiredArgsConstructor
public class RejectOrganizerRequestUseCase implements RejectOrganizerRequestPort {
    private final OrganizerPetitionRepository organizerPetitionRepository;
    private final ApplicationEventPublisher   eventPublisher;
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
        eventPublisher.publishEvent(OrganizerRejectedEvent.builder()
                .organizerPetitionId(organizerPetitionId)
                .userId(petition.getUser().getUserId())
                .occurredAt(LocalDateTime.now())
                .build());
    }
}
