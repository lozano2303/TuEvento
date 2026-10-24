package com.capysoft.tuevento.modules.notification.infrastructure.messaging;

import com.capysoft.tuevento.modules.notification.application.dto.SendNotificationCommand;
import com.capysoft.tuevento.modules.notification.application.usecase.SendNotificationUseCase;
import com.capysoft.tuevento.modules.notification.domain.model.NotificationTypeNames;
import com.capysoft.tuevento.modules.security.domain.event.OrganizerRoleRevokedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/**
 * Notifica al usuario (in-app + email) que un admin le quitó el rol de organizador,
 * incluyendo el motivo. Mismo patrón que {@link EventStatusChangedListener}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrganizerRoleRevokedListener {

    private final SendNotificationUseCase sendNotificationUseCase;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onOrganizerRoleRevoked(OrganizerRoleRevokedEvent event) {
        try {
            sendNotificationUseCase.execute(SendNotificationCommand.builder()
                    .typeName(NotificationTypeNames.ORGANIZER_ROLE_REVOKED)
                    .entityType("USER")
                    .entityId(event.getUserId().longValue())
                    .userIds(List.of(event.getUserId()))
                    .reason(event.getReason())
                    // Un usuario puede ser revocado más de una vez (si vuelve a ser aprobado)
                    .idempotencySuffix(String.valueOf(event.getOccurredAt()))
                    .build());
            log.info("Organizer-role-revoked notification sent: userId={}", event.getUserId());
        } catch (Exception e) {
            log.error("Failed to send organizer-role-revoked notification: userId={}", event.getUserId(), e);
        }
    }
}
