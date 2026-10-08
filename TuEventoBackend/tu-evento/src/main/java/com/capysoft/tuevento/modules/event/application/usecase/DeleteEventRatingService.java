package com.capysoft.tuevento.modules.event.application.usecase;

import com.capysoft.tuevento.modules.event.application.port.in.DeleteEventRatingUseCase;
import com.capysoft.tuevento.modules.event.domain.event.EventRatingDeletedEvent;
import com.capysoft.tuevento.modules.event.domain.model.EventRating;
import com.capysoft.tuevento.modules.event.domain.repository.EventRatingRepository;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Elimina físicamente un comentario/rating.
 *
 * <p>Regla C: si el comentario es principal (parentRatingId == null), se borran
 * también todas sus respuestas directas en la misma transacción.
 * El ON DELETE CASCADE de la migración 088 es la red de seguridad en BD,
 * pero aquí el borrado explícito garantiza que los contadores y el estado
 * de la aplicación queden consistentes.
 * Borrar una respuesta no afecta al padre.
 */
@Service
@RequiredArgsConstructor
public class DeleteEventRatingService implements DeleteEventRatingUseCase {

    private final EventRatingRepository     ratingRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock                     clock;

    @Override
    @Transactional
    public void execute(Long eventId, Long ratingId, Long userId) {

        EventRating rating = ratingRepository.findById(ratingId)
                .orElseThrow(() -> new NotFoundException("RATING_NOT_FOUND",
                        "Rating not found with id: " + ratingId));

        if (!rating.getEventId().equals(eventId)) {
            throw new NotFoundException("RATING_NOT_FOUND",
                    "Rating " + ratingId + " does not belong to event " + eventId);
        }
        if (!rating.getUserId().equals(userId)) {
            throw new BusinessException("RATING_ACCESS_DENIED",
                    "User " + userId + " cannot delete rating " + ratingId);
        }

        // ── Regla C: borrar respuestas primero si es principal ───────────────
        boolean isPrincipal = rating.getParentRatingId() == null;
        if (isPrincipal) {
            ratingRepository.deleteAllByParentRatingId(ratingId);
        }

        ratingRepository.deleteById(ratingId);

        eventPublisher.publishEvent(EventRatingDeletedEvent.builder()
                .ratingId(ratingId)
                .eventId(eventId)
                .userId(userId)
                .occurredAt(LocalDateTime.now(clock))
                .build());
    }
}
