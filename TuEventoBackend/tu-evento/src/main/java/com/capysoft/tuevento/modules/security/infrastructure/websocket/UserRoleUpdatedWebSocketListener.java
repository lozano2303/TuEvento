package com.capysoft.tuevento.modules.security.infrastructure.websocket;

import com.capysoft.tuevento.modules.security.domain.event.OrganizerApprovedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

/**
 * Avisa al usuario en vivo que su rol cambió (solicitud de organizador aprobada),
 * para que el cliente renueve el JWT con POST /auth/refresh (el refresh vuelve a
 * leer el rol desde la BD, que es la fuente de verdad).
 *
 * <p>Canal: {@code /topic/users/{userId}/session}
 * <p>Payload: {@code { type: "ROLE_UPDATED", role: "ORGANIZER" }}
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserRoleUpdatedWebSocketListener {

    private static final String ORGANIZER_ROLE = "ORGANIZER";

    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onOrganizerApproved(OrganizerApprovedEvent event) {
        String topic = "/topic/users/" + event.getUserId() + "/session";
        try {
            messagingTemplate.convertAndSend(topic, Map.of("type", "ROLE_UPDATED", "role", ORGANIZER_ROLE));
            log.debug("[UserRoleUpdated] ROLE_UPDATED sent to {}", topic);
        } catch (Exception e) {
            log.error("[UserRoleUpdated] WebSocket push failed (non-breaking): userId={}, error={}",
                    event.getUserId(), e.getMessage(), e);
        }
    }
}
