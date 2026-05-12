package com.capysoft.tuevento.modules.event.domain.repository;

import com.capysoft.tuevento.modules.event.domain.model.EventRating;

import java.util.List;
import java.util.Optional;

public interface EventRatingRepository {

    EventRating save(EventRating rating);
    List<EventRating> findByEventId(Long eventId);
    Optional<EventRating> findById(Long ratingId);
    boolean existsByEventIdAndUserId(Long eventId, Long userId);
}
