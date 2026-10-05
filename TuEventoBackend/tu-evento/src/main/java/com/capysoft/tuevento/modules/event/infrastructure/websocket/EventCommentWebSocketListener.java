package com.capysoft.tuevento.modules.event.infrastructure.websocket;

import com.capysoft.tuevento.modules.event.domain.event.EventRatingAddedEvent;
import com.capysoft.tuevento.modules.profile.infrastructure.persistence.repository.ProfileJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.Map;

/**
 * Listener que escucha {@link EventRatingAddedEvent} y, tras confirmar la transacción
 * (AFTER_COMMIT), publica el comentario en tiempo real al canal WebSocket del evento.
 *
 * <p>Canal: {@code /topic/events/{eventId}/comments}
 *
 * <p>Reglas:
 * <ul>
 *   <li>Solo publica comentarios visibles ({@code isVisible = true}).
 *   <li>El payload tiene la misma forma que cada elemento del GET /ratings:
 *       ratingId, userId, authorName, rating, comment, isVisible, createdAt.
 *   <li>Usa {@code AFTER_COMMIT} para garantizar que la BD ya tiene el registro
 *       antes de notificar a los clientes — nunca publica un comentario que
 *       luego hace rollback.
 *   <li>El fallo del push WebSocket es no-bloqueante: si {@code SimpMessagingTemplate}
 *       lanza, se loguea y la operación sigue sin afectar la transacción ya confirmada.
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventCommentWebSocketListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ProfileJpaRepository profileJpaRepository;

    /**
     * Recibe el evento de dominio después de que la transacción hace commit.
     * Si el comentario no es visible, se descarta silenciosamente.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEventRatingAdded(EventRatingAddedEvent event) {
        // Comentarios ocultos no se transmiten al canal
        if (!Boolean.TRUE.equals(event.getIsVisible())) {
            log.debug("[EventComment] Skipping non-visible comment ratingId={}", event.getRatingId());
            return;
        }

        String topic = "/topic/events/" + event.getEventId() + "/comments";

        try {
            String authorName = profileJpaRepository.findByUserId(event.getUserId().intValue())
                    .map(p -> p.getFullName())
                    .orElse("Usuario");

            // Payload idéntico a EventRatingResponse para que el frontend
            // no necesite transformar la respuesta REST vs la del WebSocket
            Map<String, Object> payload = new HashMap<>();
            payload.put("ratingId",    event.getRatingId());
            payload.put("userId",      event.getUserId());
            payload.put("authorName",  authorName);
            payload.put("rating",      event.getRating());
            payload.put("comment",     event.getComment());
            payload.put("isVisible",   event.getIsVisible());
            payload.put("createdAt",   event.getOccurredAt() != null
                    ? event.getOccurredAt().toString() : null);

            messagingTemplate.convertAndSend(topic, payload);

            log.debug("[EventComment] Comment broadcast to {}: ratingId={}, userId={}",
                    topic, event.getRatingId(), event.getUserId());

        } catch (Exception e) {
            // El push WebSocket es una mejora UX; no puede romper el flujo ya confirmado
            log.error("[EventComment] WebSocket push failed (non-breaking): ratingId={}, error={}",
                    event.getRatingId(), e.getMessage(), e);
        }
    }
}
