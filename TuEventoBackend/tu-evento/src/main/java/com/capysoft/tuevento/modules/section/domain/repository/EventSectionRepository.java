package com.capysoft.tuevento.modules.section.domain.repository;

import com.capysoft.tuevento.modules.section.domain.model.EventSection;

import java.util.List;
import java.util.Optional;

public interface EventSectionRepository {
    List<EventSection> findAllByEventId(Integer eventId);
    Optional<EventSection> findById(Integer eventSectionId);
    boolean existsByEventIdAndSectionTypeId(Integer eventId, Integer sectionTypeId);
    EventSection save(EventSection eventSection);
    void deleteById(Integer eventSectionId);
}
