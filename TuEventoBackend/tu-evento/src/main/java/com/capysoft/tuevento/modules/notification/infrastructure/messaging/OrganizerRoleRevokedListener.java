package com.capysoft.tuevento.modules.notification.infrastructure.messaging;

import com.capysoft.tuevento.modules.notification.application.dto.SendNotificationCommand;
import com.capysoft.tuevento.modules.notification.application.port.out.NotificationSender;
import com.capysoft.tuevento.modules.notification.domain.model.NotificationEntityTypes;
import com.capysoft.tuevento.modules.notification.domain.model.NotificationTypeNames;
import com.capysoft.tuevento.modules.security.domain.event.OrganizerRoleRevokedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Listens to {@link OrganizerRoleRevokedEvent} and:
 * <ol>
 *   <li>Sends an in-app + email notification via the standard notification pipeline
 *       (which includes automatic translation to the user's preferred language),
 *       including the reason written by the admin.</li>
 *   <li>Pushes a dedicated WebSocket message to
 *       {@code /topic/role-change/{userId}} so the frontend can react immediately:
 *       refresh the JWT, update localStorage, and show the one-time revocation modal.</li>
 * </ol>
 *
 * <p>Pattern: {@code @TransactionalEventListener(AFTER_COMMIT)} + {@code @Async}
 * + {@code @Transactional(REQUIRES_NEW)} — same as all other notification listeners.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrganizerRoleRevokedListener {

    private final NotificationSender notificationSender;
    private final SimpMessagingTemplate   messagingTemplate;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onOrganizerRoleRevoked(OrganizerRoleRevokedEvent event) {
        try {
            log.info("[OrganizerRoleRevokedListener] Handling revocation for userId={}, alias={}",
                    event.getUserId(), event.getAlias());

            // ── 1. Send in-app + email notification ──────────────────────────
            // idempotencySuffix uses timestamp millis so a re-revocation after
            // a re-grant would still deliver a fresh notification.
            String suffix = String.valueOf(event.getOccurredAt().toEpochSecond(
                    java.time.ZoneOffset.UTC));

            SendNotificationCommand command = SendNotificationCommand.builder()
                    .typeName(NotificationTypeNames.ORGANIZER_ROLE_REVOKED)
                    .entityType(NotificationEntityTypes.USER)
                    .entityId(event.getUserId().longValue())
                    .userIds(List.of(event.getUserId()))
                    .reason(event.getReason())
                    .idempotencySuffix(suffix)
                    .build();

            notificationSender.send(command);

            log.info("[OrganizerRoleRevokedListener] Notification sent to userId={}", event.getUserId());

            // ── 2. Push WebSocket role-change signal ─────────────────────────
            // The frontend subscribes to /topic/role-change/{userId}.
            // On receiving this message it:
            //   a) calls /api/v1/auth/refresh to get a new JWT with the USER role
            //   b) updates localStorage (token, role)
            //   c) shows the one-time "you are no longer an organizer" modal
            Map<String, Object> wsPayload = new HashMap<>();
            wsPayload.put("type",       "ROLE_REVOKED");
            wsPayload.put("newRole",    "USER");
            wsPayload.put("userId",     event.getUserId());
            wsPayload.put("occurredAt", event.getOccurredAt().toString());
            // El motivo no viaja por WS: el usuario lo recibe en la notificación autenticada

            String destination = "/topic/role-change/" + event.getUserId();
            messagingTemplate.convertAndSend(destination, wsPayload);

            log.info("[OrganizerRoleRevokedListener] WS role-change push sent to {}", destination);

        } catch (Exception e) {
            log.error("[OrganizerRoleRevokedListener] Unexpected error for userId={}: {}",
                    event.getUserId(), e.getMessage(), e);
            // Never re-throw — the revoke transaction already committed.
        }
    }
}
