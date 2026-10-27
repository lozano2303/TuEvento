package com.capysoft.tuevento.modules.security.application.usecase;

import com.capysoft.tuevento.modules.security.application.dto.OrganizerRoleRevokedMessage;
import com.capysoft.tuevento.modules.security.application.port.in.RevokeOrganizerRolePort;
import com.capysoft.tuevento.modules.security.application.port.out.TokenRevocationPort;
import com.capysoft.tuevento.modules.security.domain.event.OrganizerRoleRevokedEvent;
import com.capysoft.tuevento.modules.security.domain.repository.OrganizerRoleRevocationRepository;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.repository.RoleJpaRepository;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.repository.UserJpaRepository;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import com.capysoft.tuevento.shared.infrastructure.outbox.OutboxEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Quita el rol de organizador a un usuario y lo devuelve a USER.
 *
 * <p>Trabaja directo con las entidades JPA (sin los mappers de dominio) para evitar
 * errores de entidades detached. El motivo queda en organizer_role_revocation y viaja
 * en {@link OrganizerRoleRevokedEvent}: el listener lo notifica al usuario y le envía
 * ROLE_REVOKED por /topic/role-change/{userId}.
 *
 * <p>Identidad no toca los eventos del organizador. En la misma transacción que el cambio de rol
 * deja {@code OrganizerRoleRevoked} en el outbox (topic {@code tuevento.user}); Catálogo lo consume
 * y cierra sus eventos: los PUBLISHED se cancelan (con reembolso a los compradores) y los
 * PENDING_REVIEW se rechazan. Si el cierre falla se reintenta allí, sin deshacer la revocación.
 */
@Service
@RequiredArgsConstructor
public class RevokeOrganizerRoleUseCase implements RevokeOrganizerRolePort {

    private static final int REASON_MIN_LENGTH = 10;

    private final UserJpaRepository                 userJpaRepository;
    private final RoleJpaRepository                 roleJpaRepository;
    private final ApplicationEventPublisher         eventPublisher;
    private final OrganizerRoleRevocationRepository revocationRepository;
    private final OutboxEventPublisher              outboxEventPublisher;
    private final TokenRevocationPort               tokenRevocationPort;
    private final Clock                             clock;

    @Override
    @Transactional
    public void revoke(Integer userId, String reason, Integer adminUserId) {
        String trimmedReason = reason == null ? "" : reason.trim();
        if (trimmedReason.length() < REASON_MIN_LENGTH) {
            throw new BusinessException("REVOCATION_REASON_REQUIRED",
                    "A reason of at least " + REASON_MIN_LENGTH + " characters is required");
        }

        // Bloquea al usuario: dos revocaciones simultáneas no pueden pasar ambas la comprobación del rol
        var userEntity = userJpaRepository.findByIdEagerForUpdate(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));

        if (userEntity.getRole() == null || !"ORGANIZER".equals(userEntity.getRole().getCode())) {
            throw new BusinessException("USER_NOT_ORGANIZER",
                    "User " + userId + " does not have the ORGANIZER role");
        }

        var userRole = roleJpaRepository.findByCode("USER")
                .orElseThrow(() -> new NotFoundException("ROLE_NOT_FOUND", "USER role not found"));

        userEntity.setRole(userRole);
        userJpaRepository.save(userEntity);
        Long revocationId = revocationRepository.record(userId, adminUserId, trimmedReason);

        Instant now = clock.instant();
        // Sus access tokens con rol ORGANIZER dejan de valer en el monolito y en los servicios; al
        // renovar recibe uno con USER. Si la lista no responde, la revocación entera se deshace.
        tokenRevocationPort.revokeUser(userId, now);
        outboxEventPublisher.publish(OrganizerRoleRevokedMessage.AGGREGATE_TYPE, String.valueOf(userId),
                OrganizerRoleRevokedMessage.EVENT_TYPE,
                new OrganizerRoleRevokedMessage(revocationId, userId, userEntity.getAlias(), adminUserId,
                        trimmedReason, now.toString()),
                eventIdOf(revocationId));

        eventPublisher.publishEvent(OrganizerRoleRevokedEvent.builder()
                .userId(userId)
                .alias(userEntity.getAlias())
                .revokedBy(adminUserId)
                .reason(trimmedReason)
                .occurredAt(LocalDateTime.ofInstant(now, ZoneId.systemDefault()))
                .build());
    }

    /** Un eventId por revocación: el mismo valor en cualquier reenvío, distinto en cada revocación. */
    static UUID eventIdOf(Long revocationId) {
        return UUID.nameUUIDFromBytes(("OrganizerRoleRevoked:" + revocationId).getBytes(StandardCharsets.UTF_8));
    }
}
