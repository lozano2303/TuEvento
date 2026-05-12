package com.capysoft.tuevento.modules.event.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.event.infrastructure.persistence.entity.EventLayoutEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EventLayoutJpaRepository extends JpaRepository<EventLayoutEntity, Long> {

    Optional<EventLayoutEntity> findByEventId(Long eventId);
}
