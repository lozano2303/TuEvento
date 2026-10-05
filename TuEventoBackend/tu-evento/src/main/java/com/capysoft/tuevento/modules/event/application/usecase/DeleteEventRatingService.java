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

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DeleteEventRatingService implements DeleteEventRatingUseCase {

    private final EventRatingRepository ratingRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Borra físicamente el rating indicado tras validar:
     * <ol>
     *   <li>El rating existe.</li>
     *   <li>El rating pertenece al evento indicado (evita borrar ratings de otro evento).</li>
     *   <li>El rating pertenece al usuario autenticado (ownership).</li>
     * </ol>
     *
     * <p>Tras el borrado publica {@link EventRatingDeletedEvent} dentro de la transacción
     * para que el listener {@code @TransactionalEventListener(AFTER_COMMIT)} lo reciba
     * solo cuando el commit ha completado.</p>
     *
     * <p>Al eliminar su propio rating, el usuario queda libre para volver a comentar
     * en el mismo evento porque {@code existsByEventIdAndUserId} ya no retorna {@code true}.</p>
     */
    @Override
    @Transactional
    public void execute(Long eventId, Long ratingId, Long userId) {
        EventRating rating = ratingRepository.findById(ratingId)
                .orElseThrow(() -> new NotFoundException("RATING_NOT_FOUND",
                        "Rating not found with id: " + ratingId));

        // 404 si existe pero pertenece a otro evento
        if (!rating.getEventId().equals(eventId)) {
            throw new NotFoundException("RATING_NOT_FOUND",
                    "Rating " + ratingId + " does not belong to event " + eventId);
        }

        // 403 si existe y pertenece al evento pero es de otra persona
        if (!rating.getUserId().equals(userId)) {
            throw new BusinessException("RATING_ACCESS_DENIED",
                    "User " + userId + " cannot delete rating " + ratingId);
        }

        ratingRepository.deleteById(ratingId);

        eventPublisher.publishEvent(EventRatingDeletedEvent.builder()
                .ratingId(ratingId)
                .eventId(eventId)
                .userId(userId)
                .occurredAt(LocalDateTime.now())
                .build());
    }
}
