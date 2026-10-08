package com.capysoft.tuevento.modules.event.domain.repository;

import com.capysoft.tuevento.modules.event.domain.model.EventRating;

import java.util.List;
import java.util.Optional;

public interface EventRatingRepository {

    EventRating save(EventRating rating);

    /** Todos los comentarios del evento, orden createdAt DESC, ratingId DESC. */
    List<EventRating> findByEventIdOrderByCreatedAtDesc(Long eventId);

    Optional<EventRating> findById(Long ratingId);

    /** Último comentario del usuario en el evento (para antispam R5). */
    Optional<EventRating> findLastByEventIdAndUserId(Long eventId, Long userId);

    /** ¿Tiene ya el usuario un comentario principal con rating != null en este evento? (R3) */
    boolean existsRatedCommentByEventIdAndUserId(Long eventId, Long userId);

    void deleteById(Long ratingId);

    /** Borra todas las respuestas directas del comentario principal indicado. */
    void deleteAllByParentRatingId(Long parentRatingId);

    /** Lista todas las respuestas directas de un comentario principal. */
    List<EventRating> findByParentRatingId(Long parentRatingId);
}
