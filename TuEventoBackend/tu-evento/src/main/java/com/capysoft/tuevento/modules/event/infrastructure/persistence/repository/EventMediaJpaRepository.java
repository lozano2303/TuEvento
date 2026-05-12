package com.capysoft.tuevento.modules.event.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.event.infrastructure.persistence.entity.EventMediaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventMediaJpaRepository extends JpaRepository<EventMediaEntity, Long> {

    List<EventMediaEntity> findByEventId(Long eventId);
}
