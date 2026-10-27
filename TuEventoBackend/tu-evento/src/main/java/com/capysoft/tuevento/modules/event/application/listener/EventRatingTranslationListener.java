package com.capysoft.tuevento.modules.event.application.listener;

import com.capysoft.tuevento.shared.application.translation.TranslationRequests;
import com.capysoft.tuevento.modules.event.domain.event.EventRatingAddedEvent;
import com.capysoft.tuevento.modules.event.domain.event.EventRatingDeletedEvent;
import com.capysoft.tuevento.modules.event.domain.event.EventRatingUpdatedEvent;
import com.capysoft.tuevento.modules.event.domain.repository.EventRatingTranslationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Invalida las traducciones de un comentario cuando se crea, edita o borra: se eliminan sus
 * traducciones y los trabajos pendientes, y las nuevas se generan bajo demanda. Es best-effort:
 * el cambio del comentario ya se confirmó y un fallo aquí no se propaga.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventRatingTranslationListener {

    private final EventRatingTranslationRepository eventRatingTranslationRepository;
    private final TranslationRequests translationRequests;

    /** Un comentario nuevo puede reutilizar un ratingId: se limpian traducciones previas. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEventRatingAdded(EventRatingAddedEvent event) {
        invalidate(event.getRatingId(), "added");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEventRatingUpdated(EventRatingUpdatedEvent event) {
        invalidate(event.getRatingId(), "updated");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEventRatingDeleted(EventRatingDeletedEvent event) {
        invalidate(event.getRatingId(), "deleted");
    }

    private void invalidate(Long ratingId, String change) {
        try {
            eventRatingTranslationRepository.deleteByRating(ratingId.intValue());
            int deletedJobs = translationRequests.discard("event_rating", ratingId);
            log.info("[EventRatingTranslationListener] Rating {} {} - deleted its translations and {} pending jobs",
                    ratingId, change, deletedJobs);
        } catch (Exception e) {
            log.error("[EventRatingTranslationListener] Failed to invalidate translations of rating {} ({}): {}",
                    ratingId, change, e.getMessage(), e);
        }
    }
}
