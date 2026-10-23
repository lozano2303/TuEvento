package com.capysoft.tuevento.modules.event.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.event.application.mapper.EventStatusLogEntityMapper;
import com.capysoft.tuevento.modules.event.domain.model.EventStatusLog;
import com.capysoft.tuevento.modules.event.domain.repository.EventStatusLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class EventStatusLogRepositoryImpl implements EventStatusLogRepository {

    private final EventStatusLogJpaRepository jpaRepository;
    private final EventStatusLogEntityMapper mapper;

    @Override
    public EventStatusLog save(EventStatusLog log) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(log)));
    }

    @Override
    public List<EventStatusLog> findByEventId(Long eventId) {
        return jpaRepository.findByEventId(eventId).stream().map(mapper::toDomain).toList();
    }
}
