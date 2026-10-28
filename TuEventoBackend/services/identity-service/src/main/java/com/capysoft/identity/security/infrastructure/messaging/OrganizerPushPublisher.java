package com.capysoft.identity.security.infrastructure.messaging;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.capysoft.identity.security.domain.event.OrganizerApprovedEvent;
import com.capysoft.identity.security.domain.event.OrganizerPetitionCreatedEvent;
import com.capysoft.identity.security.domain.event.OrganizerRejectedEvent;
import com.capysoft.identity.shared.infrastructure.messaging.LivePushSender;

import lombok.RequiredArgsConstructor;

/**
 * Avisos en vivo de las solicitudes de organizador (los mismos que daba el monolito):
 * <ul>
 *   <li>al aprobar, {@code ROLE_UPDATED} al canal de sesión del usuario, para que renueve su token
 *       y vea el rol ORGANIZER sin volver a iniciar sesión;</li>
 *   <li>al crear, aprobar o rechazar, los contadores de pendientes del panel de admin cambian: el
 *       monolito los recalcula (incluyen eventos en revisión) y los publica en
 *       {@code /topic/admin/pending-counts}.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class OrganizerPushPublisher {

    static final String ADMIN_PENDING_COUNTS = "/topic/admin/pending-counts";
    static final String PENDING_COUNTS_CHANGED = "PENDING_COUNTS_CHANGED";

    private final LivePushSender livePushSender;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onApproved(OrganizerApprovedEvent event) {
        if (event.getUserId() != null) {
            livePushSender.send(String.valueOf(event.getUserId()), "/topic/users/" + event.getUserId() + "/session",
                    Map.of("type", "ROLE_UPDATED", "role", "ORGANIZER"));
        }
        pendingCountsChanged();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onCreated(OrganizerPetitionCreatedEvent event) {
        pendingCountsChanged();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onRejected(OrganizerRejectedEvent event) {
        pendingCountsChanged();
    }

    private void pendingCountsChanged() {
        livePushSender.send("admin", ADMIN_PENDING_COUNTS, Map.of("type", PENDING_COUNTS_CHANGED));
    }
}
