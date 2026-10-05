package com.capysoft.tuevento.modules.event.infrastructure.websocket;

import com.capysoft.tuevento.modules.event.domain.event.EventRatingDeletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

/**
 * Listener que escucha {@link EventRatingDeletedEvent} y, tras confirmar la transacción
 * (AFTER_COMMIT), notifica a todos los clientes suscritos que deben retirar el comentario
 * de su lista local.
 *
 * <p>Canal: {@code /topic/events/{eventId}/comments/deleted}
 *
 * <p>Payload: {@code { "ratingId": <id> }} — mínimo e idempotente en el cliente.
 *
 * <p>El push WebSocket es no-bloqueante: si {@code SimpMessagingTemplate} lanza,
 * se loguea y el flujo continúa sin afectar la transacción ya confirmada.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventRatingDeletedWebSocketListener {

    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEventRatingDeleted(EventRatingDeletedEvent event) {
        String topic = "/topic/events/" + event.getEventId() + "/comments/deleted";

        try {
            messagingTemplate.convertAndSend(topic, Map.of("ratingId", event.getRatingId()));

            log.debug("[EventComment] Deletion broadcast to {}: ratingId={}, userId={}",
                    topic, event.getRatingId(), event.getUserId());

        } catch (Exception e) {
            log.error("[EventComment] WebSocket deletion push failed (non-breaking): ratingId={}, error={}",
                    event.getRatingId(), e.getMessage(), e);
        }
    }
}
