package com.capysoft.tuevento.modules.event.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.event.application.mapper.EventEntityMapper;
import com.capysoft.tuevento.modules.event.domain.model.Event;
import com.capysoft.tuevento.modules.event.domain.model.EventStatus;
import com.capysoft.tuevento.modules.event.domain.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class EventRepositoryImpl implements EventRepository {

    private final EventJpaRepository jpaRepository;
    private final EventEntityMapper mapper;

    @Override
    public Event save(Event event) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(event)));
    }

    @Override
    public Optional<Event> findById(Long eventId) {
        return jpaRepository.findById(eventId).map(mapper::toDomain);
    }

    @Override
    public List<Event> findByUserId(Long userId) {
        return jpaRepository.findByUserId(userId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Event> findBySiteId(Long siteId) {
        return jpaRepository.findBySiteId(siteId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Event> findByStatus(EventStatus status) {
        return jpaRepository.findByStatus(status).stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existsByEventNameAndStartDateAndSiteId(String eventName, LocalDate startDate, Long siteId) {
        return jpaRepository.existsByEventNameAndStartDateAndSiteId(eventName, startDate, siteId);
    }

    @Override
    public void delete(Long eventId) {
        jpaRepository.deleteById(eventId);
    }
}
