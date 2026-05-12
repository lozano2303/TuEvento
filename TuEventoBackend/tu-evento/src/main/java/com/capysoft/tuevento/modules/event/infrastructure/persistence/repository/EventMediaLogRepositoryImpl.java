package com.capysoft.tuevento.modules.event.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.event.application.mapper.EventMediaLogEntityMapper;
import com.capysoft.tuevento.modules.event.domain.model.EventMediaLog;
import com.capysoft.tuevento.modules.event.domain.repository.EventMediaLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class EventMediaLogRepositoryImpl implements EventMediaLogRepository {

    private final EventMediaLogJpaRepository jpaRepository;
    private final EventMediaLogEntityMapper mapper;

    @Override
    public EventMediaLog save(EventMediaLog log) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(log)));
    }

    @Override
    public List<EventMediaLog> findByEventId(Long eventId) {
        return jpaRepository.findByEventId(eventId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public int findNextVersionByEventId(Long eventId) {
        return jpaRepository.findNextVersionByEventId(eventId);
    }
}
