package com.capysoft.tuevento.modules.language.application.listener;

import com.capysoft.tuevento.modules.event.domain.event.EventRatingAddedEvent;
import com.capysoft.tuevento.modules.event.domain.event.EventRatingDeletedEvent;
import com.capysoft.tuevento.modules.event.domain.event.EventRatingUpdatedEvent;
import com.capysoft.tuevento.modules.language.domain.repository.EventRatingTranslationRepository;
import com.capysoft.tuevento.modules.language.domain.repository.TranslationJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listener que gestiona traducciones de comentarios/ratings de eventos.
 * 
 * Cuando se agrega o edita un comentario:
 * 1. Elimina traducciones existentes (si hay cambios en el texto)
 * 2. Elimina jobs de traducción pendientes
 * 3. Las nuevas traducciones se generarán on-demand cuando un usuario solicite
 *    el comentario en otro idioma (lazy loading)
 * 
 * Cuando se elimina un comentario:
 * 1. Elimina todas sus traducciones
 * 2. Elimina jobs de traducción pendientes
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventRatingTranslationListener {

    private final EventRatingTranslationRepository eventRatingTranslationRepository;
    private final TranslationJobRepository translationJobRepository;

    /**
     * Escucha cuando se agrega un nuevo comentario/rating.
     * Se eliminan traducciones previas (por si el ratingId fue reutilizado) y jobs pendientes.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEventRatingAdded(EventRatingAddedEvent event) {
        try {
            log.debug("[EventRatingTranslationListener] Rating {} added, invalidating any existing translations", 
                    event.getRatingId());
            
            // Eliminar traducciones existentes (por si el ID fue reutilizado)
            eventRatingTranslationRepository.deleteByRating(event.getRatingId().intValue());
            
            // Eliminar jobs pendientes
            int deletedJobs = translationJobRepository.deleteByEntity("event_rating", event.getRatingId().intValue());
            
            log.info("[EventRatingTranslationListener] Rating {} added - deleted any old translations and {} pending jobs. New translations will be generated on-demand.",
                    event.getRatingId(), deletedJobs);
            
        } catch (Exception e) {
            log.error("[EventRatingTranslationListener] Failed to process rating addition for rating {}: {}", 
                    event.getRatingId(), e.getMessage(), e);
            // Non-blocking: el comentario ya fue guardado exitosamente
        }
    }

    /**
     * Escucha cuando se edita un comentario existente.
     * Se eliminan todas las traducciones y jobs para que se regeneren con el nuevo texto.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEventRatingUpdated(EventRatingUpdatedEvent event) {
        try {
            log.info("[EventRatingTranslationListener] Rating {} updated, invalidating translations", 
                    event.getRatingId());
            
            // Eliminar todas las traducciones existentes
            eventRatingTranslationRepository.deleteByRating(event.getRatingId().intValue());
            
            // Eliminar todos los jobs pendientes
            int deletedJobs = translationJobRepository.deleteByEntity("event_rating", event.getRatingId().intValue());
            
            log.info("[EventRatingTranslationListener] Rating {} updated - deleted translations and {} pending jobs. New translations will be generated on-demand.",
                    event.getRatingId(), deletedJobs);
            
        } catch (Exception e) {
            log.error("[EventRatingTranslationListener] Failed to process rating update for rating {}: {}", 
                    event.getRatingId(), e.getMessage(), e);
            // Non-blocking: el comentario ya fue actualizado exitosamente
        }
    }

    /**
     * Escucha cuando se elimina un comentario.
     * Se limpian todas las traducciones y jobs asociados.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEventRatingDeleted(EventRatingDeletedEvent event) {
        try {
            log.info("[EventRatingTranslationListener] Rating {} deleted, cleaning up translations", 
                    event.getRatingId());
            
            // Eliminar todas las traducciones
            eventRatingTranslationRepository.deleteByRating(event.getRatingId().intValue());
            
            // Eliminar todos los jobs pendientes
            int deletedJobs = translationJobRepository.deleteByEntity("event_rating", event.getRatingId().intValue());
            
            log.info("[EventRatingTranslationListener] Rating {} deleted - cleaned up translations and {} pending jobs.",
                    event.getRatingId(), deletedJobs);
            
        } catch (Exception e) {
            log.error("[EventRatingTranslationListener] Failed to clean up translations for deleted rating {}: {}", 
                    event.getRatingId(), e.getMessage(), e);
            // Non-blocking: el comentario ya fue eliminado
        }
    }
}
