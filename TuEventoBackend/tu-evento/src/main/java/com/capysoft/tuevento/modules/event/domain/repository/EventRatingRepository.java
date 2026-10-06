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

    /** ¿Tiene ya el usuario un comentario con rating != null en este evento? (R3) */
    boolean existsRatedCommentByEventIdAndUserId(Long eventId, Long userId);
}
