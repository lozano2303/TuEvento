package com.capysoft.tuevento.modules.security.application.usecase;

import com.capysoft.tuevento.modules.security.application.port.in.ApproveOrganizerRequestPort;
import com.capysoft.tuevento.modules.security.domain.event.OrganizerApprovedEvent;
import com.capysoft.tuevento.modules.security.domain.model.OrganizerPetition;
import com.capysoft.tuevento.modules.security.domain.repository.OrganizerPetitionRepository;
import com.capysoft.tuevento.modules.security.domain.repository.RoleRepository;
import com.capysoft.tuevento.modules.security.domain.repository.UserRepository;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ApproveOrganizerRequestUseCase implements ApproveOrganizerRequestPort {

    private final OrganizerPetitionRepository organizerPetitionRepository;
    private final UserRepository              userRepository;
    private final RoleRepository              roleRepository;
    private final ApplicationEventPublisher   eventPublisher;

    @Override
    @Transactional
    public void approve(Integer organizerPetitionId) {
        OrganizerPetition petition = organizerPetitionRepository.findById(organizerPetitionId)
                .orElseThrow(() -> new NotFoundException("PETITION_NOT_FOUND", "Organizer petition not found"));

        if (!"PENDING".equals(petition.getStatus())) {
            throw new BusinessException("PETITION_NOT_PENDING", "Only pending petitions can be approved");
        }

        petition.setStatus("APPROVED");
        organizerPetitionRepository.save(petition);

        var organizerRole = roleRepository.findByCode("ORGANIZER")
                .orElseThrow(() -> new NotFoundException("ROLE_NOT_FOUND", "ORGANIZER role not found"));

        var user = petition.getUser();
        user.setRole(organizerRole);
        userRepository.save(user);

        eventPublisher.publishEvent(OrganizerApprovedEvent.builder()
                .organizerPetitionId(organizerPetitionId)
                .userId(user.getUserId())
                .alias(user.getAlias())
                .occurredAt(LocalDateTime.now())
                .build());
    }
}
