package com.capysoft.tuevento.modules.security.application.usecase;

import com.capysoft.tuevento.modules.event.application.dto.request.ChangeEventStatusRequest;
import com.capysoft.tuevento.modules.event.application.port.in.AdminChangeEventStatusPort;
import com.capysoft.tuevento.modules.event.domain.event.EventStatusChangedEvent;
import com.capysoft.tuevento.modules.event.domain.model.Event;
import com.capysoft.tuevento.modules.event.domain.model.EventStatus;
import com.capysoft.tuevento.modules.event.domain.repository.EventRepository;
import com.capysoft.tuevento.modules.security.application.port.in.RevokeOrganizerRolePort;
import com.capysoft.tuevento.modules.security.domain.event.OrganizerRoleRevokedEvent;
import com.capysoft.tuevento.modules.security.domain.repository.OrganizerRoleRevocationRepository;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.repository.RoleJpaRepository;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.repository.UserJpaRepository;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Quita el rol de organizador a un usuario y lo devuelve a USER.
 *
 * <p>Trabaja directo con las entidades JPA (sin los mappers de dominio) para evitar
 * errores de entidades detached. El motivo queda en organizer_role_revocation y viaja
 * en {@link OrganizerRoleRevokedEvent}: el listener lo notifica al usuario y le envía
 * ROLE_REVOKED por /topic/role-change/{userId}.
 *
 * <p>Sus eventos activos se cierran en la misma transacción: los PUBLISHED se cancelan
 * (los compradores reciben el reembolso en su cartera y quien esté viendo el evento ve el
 * aviso con la causa ORGANIZER_REVOKED) y los PENDING_REVIEW se rechazan con el motivo.
 */
@Service
@RequiredArgsConstructor
public class RevokeOrganizerRoleUseCase implements RevokeOrganizerRolePort {

    private static final int REASON_MIN_LENGTH = 10;

    private final UserJpaRepository                 userJpaRepository;
    private final RoleJpaRepository                 roleJpaRepository;
    private final ApplicationEventPublisher         eventPublisher;
    private final OrganizerRoleRevocationRepository revocationRepository;
    private final EventRepository                   eventRepository;
    private final AdminChangeEventStatusPort        adminChangeEventStatusPort;

    @Override
    @Transactional
    public void revoke(Integer userId, String reason, Integer adminUserId) {
        String trimmedReason = reason == null ? "" : reason.trim();
        if (trimmedReason.length() < REASON_MIN_LENGTH) {
            throw new BusinessException("REVOCATION_REASON_REQUIRED",
                    "A reason of at least " + REASON_MIN_LENGTH + " characters is required");
        }

        var userEntity = userJpaRepository.findByIdEager(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));

        if (userEntity.getRole() == null || !"ORGANIZER".equals(userEntity.getRole().getCode())) {
            throw new BusinessException("USER_NOT_ORGANIZER",
                    "User " + userId + " does not have the ORGANIZER role");
        }

        var userRole = roleJpaRepository.findByCode("USER")
                .orElseThrow(() -> new NotFoundException("ROLE_NOT_FOUND", "USER role not found"));

        userEntity.setRole(userRole);
        userJpaRepository.save(userEntity);
        revocationRepository.record(userId, adminUserId, trimmedReason);
        closeActiveEvents(userId, adminUserId, trimmedReason);

        eventPublisher.publishEvent(OrganizerRoleRevokedEvent.builder()
                .userId(userId)
                .alias(userEntity.getAlias())
                .revokedBy(adminUserId)
                .reason(trimmedReason)
                .occurredAt(LocalDateTime.now())
                .build());
    }

    private void closeActiveEvents(Integer userId, Integer adminUserId, String reason) {
        Long adminId = adminUserId == null ? null : adminUserId.longValue();
        for (Event event : eventRepository.findByUserId(userId.longValue())) {
            if (event.getStatus() == EventStatus.PUBLISHED) {
                adminChangeEventStatusPort.execute(event.getEventId(),
                        ChangeEventStatusRequest.builder()
                                .newStatus(EventStatus.CANCELLED)
                                .reason("Rol de organizador revocado: " + reason)
                                .build(),
                        adminId, EventStatusChangedEvent.CAUSE_ORGANIZER_REVOKED);
            } else if (event.getStatus() == EventStatus.PENDING_REVIEW) {
                adminChangeEventStatusPort.execute(event.getEventId(),
                        ChangeEventStatusRequest.builder()
                                .newStatus(EventStatus.REJECTED)
                                .reason("Se revocó tu rol de organizador: " + reason)
                                .build(),
                        adminId, EventStatusChangedEvent.CAUSE_ORGANIZER_REVOKED);
            }
        }
    }
}
