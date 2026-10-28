package com.capysoft.tuevento.modules.identitybridge.websocket;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.capysoft.tuevento.modules.event.domain.event.EventStatusChangedEvent;
import com.capysoft.tuevento.modules.identitybridge.client.IdentityServiceClient;
import com.capysoft.tuevento.modules.identitybridge.port.AdminPanelSourcesPort;
import com.capysoft.tuevento.shared.infrastructure.websocket.AdminPendingCountsChangedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Publica en {@code /topic/admin/pending-counts} los contadores de pendientes del panel de admin
 * (mismo payload de siempre: {@code {organizerRequests, eventsInReview}}) cuando cambian:
 * <ul>
 *   <li>una solicitud de organizador se crea, aprueba o rechaza en identity-service (llega como
 *       {@link AdminPendingCountsChangedEvent} por el relay);</li>
 *   <li>un evento entra o sale de PENDING_REVIEW.</li>
 * </ul>
 * Las solicitudes pendientes las cuenta Identidad; los eventos en revisión, Catálogo.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminPendingCountsWebSocketListener {

    static final String TOPIC = "/topic/admin/pending-counts";
    private static final String PENDING_REVIEW = "PENDING_REVIEW";

    private final SimpMessagingTemplate messagingTemplate;
    private final IdentityServiceClient identityServiceClient;
    private final AdminPanelSourcesPort adminPanelSources;

    public record PendingCounts(long organizerRequests, long eventsInReview) {
    }

    @EventListener
    public void onChangedInIdentity(AdminPendingCountsChangedEvent event) {
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
            PendingCounts counts = new PendingCounts(identityServiceClient.pendingOrganizerRequests(),
                    adminPanelSources.eventsPendingReview());
            messagingTemplate.convertAndSend(TOPIC, counts);
            log.debug("[AdminPendingCounts] Counts broadcast to {}", TOPIC);
        } catch (Exception e) {
            // El push es una mejora UX: nunca debe romper el flujo que lo originó (ni enviar un cero falso)
            log.error("[AdminPendingCounts] Counts not broadcast: {}", e.getMessage());
        }
    }
}
