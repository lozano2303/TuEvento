package com.capysoft.tuevento.modules.security.application.usecase;

import com.capysoft.tuevento.modules.security.application.port.in.ApproveOrganizerRequestPort;
import com.capysoft.tuevento.modules.security.domain.event.OrganizerApprovedEvent;
import com.capysoft.tuevento.modules.security.domain.repository.RoleRepository;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.repository.OrganizerPetitionJpaRepository;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.repository.RoleJpaRepository;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.repository.UserJpaRepository;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApproveOrganizerRequestUseCase implements ApproveOrganizerRequestPort {

    private final OrganizerPetitionJpaRepository jpaRepository;
    private final UserJpaRepository              userJpaRepository;
    private final RoleJpaRepository              roleJpaRepository;
    private final RoleRepository                 roleRepository;
    private final ApplicationEventPublisher      eventPublisher;

    @Override
    @Transactional
    public void approve(Integer organizerPetitionId) {
        // Load the petition entity directly via JPA — bypasses the domain mapper
        // to avoid detached entity issues when saving back
        var petitionEntity = jpaRepository.findByIdEager(organizerPetitionId)
                .orElseThrow(() -> new NotFoundException("PETITION_NOT_FOUND", "Organizer petition not found"));

        if (!"PENDING".equals(petitionEntity.getStatus())) {
            throw new BusinessException("PETITION_NOT_PENDING", "Only pending petitions can be approved");
        }

        petitionEntity.setStatus("APPROVED");
        jpaRepository.save(petitionEntity);

        var organizerRole = roleRepository.findByCode("ORGANIZER")
                .orElseThrow(() -> new NotFoundException("ROLE_NOT_FOUND", "ORGANIZER role not found"));

        // Load the user entity directly with eager associations
        var userEntity = userJpaRepository.findByIdEager(petitionEntity.getUser().getUserId())
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));

        // Load the target role entity so Hibernate tracks it as managed
        var roleEntity = roleJpaRepository.findById(organizerRole.getRoleId())
                .orElseThrow(() -> new NotFoundException("ROLE_NOT_FOUND", "ORGANIZER role entity not found"));

        userEntity.setRole(roleEntity);
        userJpaRepository.save(userEntity);

        eventPublisher.publishEvent(OrganizerApprovedEvent.builder()
                .organizerPetitionId(organizerPetitionId)
                .userId(userEntity.getUserId())
                .alias(userEntity.getAlias())
                .occurredAt(java.time.LocalDateTime.now())
                .build());
    }
}
