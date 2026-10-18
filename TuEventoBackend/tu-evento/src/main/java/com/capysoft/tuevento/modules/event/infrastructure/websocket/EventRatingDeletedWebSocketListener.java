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
 * Notifica a los clientes WebSocket que un comentario fue eliminado.
 * Canal: /topic/events/{eventId}/comments/deleted
 * Payload: { "ratingId": <id> }
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
            log.debug("[EventComment] Deletion broadcast to {}: ratingId={}", topic, event.getRatingId());
        } catch (Exception e) {
            log.error("[EventComment] WebSocket deletion push failed (non-breaking): ratingId={}, error={}",
                    event.getRatingId(), e.getMessage(), e);
        }
    }
}
