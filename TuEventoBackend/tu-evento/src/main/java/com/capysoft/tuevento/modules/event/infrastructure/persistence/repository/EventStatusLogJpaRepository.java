package com.capysoft.tuevento.modules.event.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.event.infrastructure.persistence.entity.EventStatusLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventStatusLogJpaRepository extends JpaRepository<EventStatusLogEntity, Long> {

    List<EventStatusLogEntity> findByEventId(Long eventId);
}
