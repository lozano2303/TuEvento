package com.capysoft.tuevento.modules.security.infrastructure.websocket;

import com.capysoft.tuevento.modules.event.domain.event.EventStatusChangedEvent;
import com.capysoft.tuevento.modules.security.application.port.in.GetAdminPendingCountsPort;
import com.capysoft.tuevento.modules.security.domain.event.OrganizerApprovedEvent;
import com.capysoft.tuevento.modules.security.domain.event.OrganizerPetitionCreatedEvent;
import com.capysoft.tuevento.modules.security.domain.event.OrganizerRejectedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Emite los contadores de pendientes del administrador cuando cambian, tras
 * confirmar la transacción (AFTER_COMMIT) para que el conteo refleje la BD.
 *
 * <p>Canal: {@code /topic/admin/pending-counts}
 * <p>Payload: misma forma que GET /admin/pending-counts → {@code { organizerRequests, eventsInReview }}.
 *
 * <p>Disparadores:
 * <ul>
 *   <li>Solicitud de organizador creada, aprobada o rechazada.</li>
 *   <li>Cualquier transición de evento que entre o salga de PENDING_REVIEW.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminPendingCountsWebSocketListener {

    static final String TOPIC = "/topic/admin/pending-counts";
    private static final String PENDING_REVIEW = "PENDING_REVIEW";

    private final SimpMessagingTemplate     messagingTemplate;
    private final GetAdminPendingCountsPort getAdminPendingCountsPort;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPetitionCreated(OrganizerPetitionCreatedEvent event) {
        broadcast();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPetitionApproved(OrganizerApprovedEvent event) {
        broadcast();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPetitionRejected(OrganizerRejectedEvent event) {
        broadcast();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onEventStatusChanged(EventStatusChangedEvent event) {
        if (PENDING_REVIEW.equals(event.getOldStatus()) || PENDING_REVIEW.equals(event.getNewStatus())) {
            broadcast();
        }
    }

    private void broadcast() {
        try {
            messagingTemplate.convertAndSend(TOPIC, getAdminPendingCountsPort.getCounts());
            log.debug("[AdminPendingCounts] Counts broadcast to {}", TOPIC);
        } catch (Exception e) {
            // El push es una mejora UX: nunca debe romper el flujo que lo originó.
            log.error("[AdminPendingCounts] WebSocket push failed (non-breaking): {}", e.getMessage(), e);
        }
    }
}
