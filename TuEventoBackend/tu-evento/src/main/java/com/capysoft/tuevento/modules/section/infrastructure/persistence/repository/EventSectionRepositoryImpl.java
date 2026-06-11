package com.capysoft.tuevento.modules.section.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.section.domain.model.EventSection;
import com.capysoft.tuevento.modules.section.domain.repository.EventSectionRepository;
import com.capysoft.tuevento.modules.section.infrastructure.persistence.entity.EventSectionEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class EventSectionRepositoryImpl implements EventSectionRepository {

    private final EventSectionJpaRepository jpaRepository;

    @Override
    public List<EventSection> findAllByEventId(Integer eventId) {
        return jpaRepository.findAllByEventId(eventId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<EventSection> findById(Integer eventSectionId) {
        return jpaRepository.findById(eventSectionId).map(this::toDomain);
    }

    @Override
    public boolean existsByEventIdAndSectionTypeId(Integer eventId, Integer sectionTypeId) {
        return jpaRepository.existsByEventIdAndSectionTypeId(eventId, sectionTypeId);
    }

    @Override
    public EventSection save(EventSection eventSection) {
        EventSectionEntity entity = EventSectionEntity.builder()
                .eventSectionId(eventSection.getEventSectionId())
                .eventId(eventSection.getEventId())
                .sectionTypeId(eventSection.getSectionTypeId())
                .capacity(eventSection.getCapacity())
                .availableSeats(eventSection.getAvailableSeats())
                .price(eventSection.getPrice())
                .isActive(eventSection.getIsActive())
                .build();
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public void deleteById(Integer eventSectionId) {
        jpaRepository.deleteById(eventSectionId);
    }

    private EventSection toDomain(EventSectionEntity entity) {
        return EventSection.builder()
                .eventSectionId(entity.getEventSectionId())
                .eventId(entity.getEventId())
                .sectionTypeId(entity.getSectionTypeId())
                .capacity(entity.getCapacity())
                .availableSeats(entity.getAvailableSeats())
                .price(entity.getPrice())
                .isActive(entity.getIsActive())
                .build();
    }
}
