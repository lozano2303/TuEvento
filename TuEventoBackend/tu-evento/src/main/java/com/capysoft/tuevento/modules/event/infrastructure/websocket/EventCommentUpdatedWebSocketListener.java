package com.capysoft.tuevento.modules.event.infrastructure.websocket;

import com.capysoft.tuevento.modules.event.domain.event.EventRatingUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.Map;

/**
 * Listener que escucha {@link EventRatingUpdatedEvent} y, tras confirmar la transacción
 * (AFTER_COMMIT), publica el comentario actualizado en tiempo real.
 *
 * <p>Canal: {@code /topic/events/{eventId}/comments/updated}
 *
 * <p>El payload tiene la MISMA forma que cada elemento del GET /ratings:
 * ratingId, userId, authorName, rating (nullable), comment, isVisible,
 * isOrganizer, createdAt, editedAt, parentRatingId (nullable).
 * Esto permite que el frontend reemplace el comentario completo por ratingId
 * sin perder ningún campo que no cambia (rating, createdAt, isOrganizer, etc.).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventCommentUpdatedWebSocketListener {

    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEventRatingUpdated(EventRatingUpdatedEvent event) {
        String topic = "/topic/events/" + event.getEventId() + "/comments/updated";

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("ratingId",       event.getRatingId());
            payload.put("userId",         event.getUserId());
            payload.put("authorName",     event.getAuthorName());
            payload.put("rating",         event.getRating());         // nullable
            payload.put("comment",        event.getComment());
            payload.put("isVisible",      event.getIsVisible());
            payload.put("isOrganizer",    Boolean.TRUE.equals(event.getIsOrganizer()));
            payload.put("createdAt",      event.getCreatedAt() != null
                    ? event.getCreatedAt().toString() : null);
            payload.put("editedAt",       event.getEditedAt() != null
                    ? event.getEditedAt().toString() : null);
            payload.put("parentRatingId", event.getParentRatingId());  // nullable

            messagingTemplate.convertAndSend(topic, payload);
            log.debug("[EventComment] Updated broadcast to {}: ratingId={}, isOrganizer={}",
                    topic, event.getRatingId(), event.getIsOrganizer());
        } catch (Exception e) {
            log.error("[EventComment] WebSocket updated push failed (non-breaking): ratingId={}, error={}",
                    event.getRatingId(), e.getMessage(), e);
        }
    }
}
