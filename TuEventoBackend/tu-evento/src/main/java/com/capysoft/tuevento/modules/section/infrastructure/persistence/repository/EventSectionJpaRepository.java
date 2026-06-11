package com.capysoft.tuevento.modules.section.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.section.infrastructure.persistence.entity.EventSectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventSectionJpaRepository extends JpaRepository<EventSectionEntity, Integer> {
    List<EventSectionEntity> findAllByEventId(Integer eventId);
    boolean existsByEventIdAndSectionTypeId(Integer eventId, Integer sectionTypeId);
}
