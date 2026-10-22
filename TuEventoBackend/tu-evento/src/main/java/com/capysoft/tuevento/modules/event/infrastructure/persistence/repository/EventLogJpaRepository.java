package com.capysoft.tuevento.modules.event.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.event.infrastructure.persistence.entity.EventLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface EventLogJpaRepository extends JpaRepository<EventLogEntity, Long> {

    List<EventLogEntity> findByEventId(Long eventId);
    boolean existsByEventIdAndActionInAndOccurredAtBetween(Long eventId, List<String> actions, LocalDateTime start, LocalDateTime end);
}