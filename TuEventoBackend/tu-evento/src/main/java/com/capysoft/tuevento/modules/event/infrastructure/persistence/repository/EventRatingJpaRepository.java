package com.capysoft.tuevento.modules.event.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.event.infrastructure.persistence.entity.EventRatingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventRatingJpaRepository extends JpaRepository<EventRatingEntity, Long> {

    List<EventRatingEntity> findByEventId(Long eventId);
    boolean existsByEventIdAndUserId(Long eventId, Long userId);
}
