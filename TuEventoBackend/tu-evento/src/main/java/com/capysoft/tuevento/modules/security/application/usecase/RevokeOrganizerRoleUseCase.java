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
import com.capysoft.tuevento.modules.security.domain.repository.RoleRepository;
import com.capysoft.tuevento.modules.security.domain.repository.UserRepository;
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
 * <p>El motivo queda en organizer_role_revocation y se notifica al usuario. El aviso
 * ROLE_UPDATED por WebSocket hace que sus sesiones abiertas renueven el JWT con el rol nuevo.
 *
 * <p>Sus eventos activos se cierran en la misma transacción: los PUBLISHED se cancelan
 * (los compradores reciben el reembolso en su cartera y quien esté viendo el evento ve el
 * aviso con la causa ORGANIZER_REVOKED) y los PENDING_REVIEW se rechazan con el motivo.
 */
@Service
@RequiredArgsConstructor
public class RevokeOrganizerRoleUseCase implements RevokeOrganizerRolePort {

    private static final String ORGANIZER_ROLE = "ORGANIZER";
    private static final String USER_ROLE      = "USER";

    private final UserRepository                    userRepository;
    private final RoleRepository                    roleRepository;
    private final OrganizerRoleRevocationRepository revocationRepository;
    private final ApplicationEventPublisher         eventPublisher;
    private final EventRepository                   eventRepository;
    private final AdminChangeEventStatusPort        adminChangeEventStatusPort;

    @Override
    @Transactional
    public void revoke(Integer userId, String reason, Integer adminUserId) {
        String trimmedReason = reason == null ? "" : reason.trim();
        if (trimmedReason.length() < 10) {
            throw new BusinessException("REVOCATION_REASON_REQUIRED",
                    "A reason of at least 10 characters is required");
        }

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found with id: " + userId));

        if (user.getRole() == null || !ORGANIZER_ROLE.equals(user.getRole().getCode())) {
            throw new BusinessException("USER_NOT_ORGANIZER", "Only organizers can have the organizer role revoked");
        }

        var userRole = roleRepository.findByCode(USER_ROLE)
                .orElseThrow(() -> new NotFoundException("ROLE_NOT_FOUND", "USER role not found"));

        user.setRole(userRole);
        userRepository.save(user);
        revocationRepository.record(userId, adminUserId, trimmedReason);
        closeActiveEvents(userId, adminUserId, trimmedReason);

        eventPublisher.publishEvent(OrganizerRoleRevokedEvent.builder()
                .userId(userId)
                .revokedBy(adminUserId)
                .reason(trimmedReason)
                .occurredAt(LocalDateTime.now())
                .build());
    }

    private void closeActiveEvents(Integer userId, Integer adminUserId, String reason) {
        Long adminId = adminUserId.longValue();
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
