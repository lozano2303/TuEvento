package com.capysoft.tuevento.modules.event.infrastructure.websocket;

import com.capysoft.tuevento.modules.event.domain.event.EventStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.Map;

/**
 * Avisa en vivo a quienes están viendo el detalle de un evento que fue cancelado
 * (por el organizador o el admin), para que el cliente muestre un aviso y salga
 * de la pantalla del evento.
 *
 * <p>Canal: {@code /topic/events/{eventId}/status}
 * <p>Payload: {@code { type: "EVENT_CANCELLED", eventId: 12, cause?: "ORGANIZER_REVOKED" }}
 * ({@code cause} solo viene cuando el evento se canceló porque el admin quitó el rol al organizador;
 * el motivo que escribió el admin no se envía a los asistentes).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventCancelledWebSocketListener {

    private static final String CANCELLED = "CANCELLED";

    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onEventStatusChanged(EventStatusChangedEvent event) {
        if (!CANCELLED.equals(event.getNewStatus()) || event.getEventId() == null) return;
        String topic = "/topic/events/" + event.getEventId() + "/status";
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "EVENT_CANCELLED");
            payload.put("eventId", event.getEventId());
            if (event.getCause() != null) payload.put("cause", event.getCause());
            messagingTemplate.convertAndSend(topic, payload);
            log.debug("[EventCancelled] EVENT_CANCELLED sent to {}", topic);
        } catch (Exception e) {
            log.error("[EventCancelled] WebSocket push failed (non-breaking): eventId={}, error={}",
                    event.getEventId(), e.getMessage(), e);
        }
    }
}
