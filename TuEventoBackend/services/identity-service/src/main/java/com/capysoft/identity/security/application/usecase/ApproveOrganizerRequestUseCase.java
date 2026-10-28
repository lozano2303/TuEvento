package com.capysoft.identity.security.application.usecase;

import com.capysoft.identity.security.application.port.in.ApproveOrganizerRequestPort;
import com.capysoft.identity.security.domain.event.OrganizerApprovedEvent;
import com.capysoft.identity.security.infrastructure.persistence.repository.OrganizerPetitionJpaRepository;
import com.capysoft.identity.security.infrastructure.persistence.repository.RoleJpaRepository;
import com.capysoft.identity.security.infrastructure.persistence.repository.UserJpaRepository;
import com.capysoft.common.domain.exception.BusinessException;
import com.capysoft.common.domain.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApproveOrganizerRequestUseCase implements ApproveOrganizerRequestPort {

    private final OrganizerPetitionJpaRepository jpaRepository;
    private final UserJpaRepository              userJpaRepository;
    private final RoleJpaRepository              roleJpaRepository;
    private final ApplicationEventPublisher      eventPublisher;

    @Override
    @Transactional
    public void approve(Integer organizerPetitionId) {
        log.info("[Approve] Loading petition id={}", organizerPetitionId);

        var petitionEntity = jpaRepository.findByIdEager(organizerPetitionId)
                .orElseThrow(() -> new NotFoundException("PETITION_NOT_FOUND", "Organizer petition not found"));

        log.info("[Approve] Petition status={}, userId={}",
                petitionEntity.getStatus(), petitionEntity.getUser().getUserId());

        if (!"PENDING".equals(petitionEntity.getStatus())) {
            throw new BusinessException("PETITION_NOT_PENDING", "Only pending petitions can be approved");
        }

        // 1. Update petition status — entity is managed, no mapper involved
        petitionEntity.setStatus("APPROVED");
        jpaRepository.save(petitionEntity);
        log.info("[Approve] Petition saved as APPROVED");

        // 2. Find the ORGANIZER role entity (managed by Hibernate)
        var organizerRoleEntity = roleJpaRepository.findByCode("ORGANIZER")
                .orElseThrow(() -> new NotFoundException("ROLE_NOT_FOUND", "ORGANIZER role not found in DB"));
        log.info("[Approve] ORGANIZER roleId={}", organizerRoleEntity.getRoleId());

        // 3. Load the user with all lazy associations eagerly loaded
        Integer userId = petitionEntity.getUser().getUserId();
        var userEntity = userJpaRepository.findByIdEager(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found: " + userId));
        log.info("[Approve] User loaded alias={}, currentRole={}",
                userEntity.getAlias(), userEntity.getRole() != null ? userEntity.getRole().getCode() : "null");

        // 4. Assign managed role entity — no detached entity issue
        userEntity.setRole(organizerRoleEntity);
        userJpaRepository.save(userEntity);
        log.info("[Approve] User role updated to ORGANIZER");

        // 5. Publish domain event
        eventPublisher.publishEvent(OrganizerApprovedEvent.builder()
                .organizerPetitionId(organizerPetitionId)
                .userId(userId)
                .alias(userEntity.getAlias())
                .occurredAt(LocalDateTime.now())
                .build());

        log.info("[Approve] Done for userId={}", userId);
    }
}
