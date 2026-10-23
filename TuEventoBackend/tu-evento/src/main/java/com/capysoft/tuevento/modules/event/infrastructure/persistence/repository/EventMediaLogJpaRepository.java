package com.capysoft.tuevento.modules.event.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.event.infrastructure.persistence.entity.EventMediaLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EventMediaLogJpaRepository extends JpaRepository<EventMediaLogEntity, Long> {

    List<EventMediaLogEntity> findByEventId(Long eventId);

    @Query("SELECT COALESCE(MAX(e.version), 0) + 1 FROM EventMediaLogEntity e WHERE e.eventId = :eventId")
    int findNextVersionByEventId(@Param("eventId") Long eventId);
}
