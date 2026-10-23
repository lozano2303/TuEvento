package com.capysoft.tuevento.modules.security.application.usecase;

import com.capysoft.tuevento.modules.security.application.port.in.RevokeOrganizerRolePort;
import com.capysoft.tuevento.modules.security.domain.event.OrganizerRoleRevokedEvent;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.repository.RoleJpaRepository;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.repository.UserJpaRepository;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RevokeOrganizerRoleUseCase implements RevokeOrganizerRolePort {

    private final UserJpaRepository         userJpaRepository;
    private final RoleJpaRepository         roleJpaRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public void revoke(Integer userId) {
        var userEntity = userJpaRepository.findByIdEager(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));

        if (!"ORGANIZER".equals(userEntity.getRole().getCode())) {
            throw new BusinessException("USER_NOT_ORGANIZER",
                    "User " + userId + " does not have the ORGANIZER role");
        }

        var userRole = roleJpaRepository.findByCode("USER")
                .orElseThrow(() -> new NotFoundException("ROLE_NOT_FOUND", "USER role not found"));

        userEntity.setRole(userRole);
        userJpaRepository.save(userEntity);

        eventPublisher.publishEvent(OrganizerRoleRevokedEvent.builder()
                .userId(userId)
                .alias(userEntity.getAlias())
                .occurredAt(LocalDateTime.now())
                .build());
    }
}
