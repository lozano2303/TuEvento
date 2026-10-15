package com.capysoft.tuevento.modules.event.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.event.application.mapper.EventLogMapper;
import com.capysoft.tuevento.modules.event.domain.model.EventLog;
import com.capysoft.tuevento.modules.event.domain.repository.EventLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class EventLogRepositoryImpl implements EventLogRepository {

    private final EventLogJpaRepository jpaRepository;
    private final EventLogMapper mapper;

    @Override
    public EventLog save(EventLog eventLog) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(eventLog)));
    }

    @Override
    public List<EventLog> findByEventId(Long eventId) {
        return jpaRepository.findByEventId(eventId).stream().map(mapper::toDomain).toList();
    }
    
    @Override
    public boolean existsByEventIdAndActionInAndOccurredAtBetween(Long eventId, List<String> actions, LocalDateTime start, LocalDateTime end) {
        return jpaRepository.existsByEventIdAndActionInAndOccurredAtBetween(eventId, actions, start, end);
    }
}