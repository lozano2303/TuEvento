package com.capysoft.tuevento.modules.event.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.event.application.mapper.EventRatingEntityMapper;
import com.capysoft.tuevento.modules.event.domain.model.EventRating;
import com.capysoft.tuevento.modules.event.domain.repository.EventRatingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class EventRatingRepositoryImpl implements EventRatingRepository {

    private final EventRatingJpaRepository jpaRepository;
    private final EventRatingEntityMapper mapper;

    @Override
    public EventRating save(EventRating rating) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(rating)));
    }

    @Override
    public List<EventRating> findByEventId(Long eventId) {
        return jpaRepository.findByEventId(eventId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<EventRating> findById(Long ratingId) {
        return jpaRepository.findById(ratingId).map(mapper::toDomain);
    }

    @Override
    public boolean existsByEventIdAndUserId(Long eventId, Long userId) {
        return jpaRepository.existsByEventIdAndUserId(eventId, userId);
    }
}
