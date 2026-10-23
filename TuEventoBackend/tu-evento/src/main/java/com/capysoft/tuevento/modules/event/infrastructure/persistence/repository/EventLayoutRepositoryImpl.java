package com.capysoft.tuevento.modules.event.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.event.application.mapper.EventLayoutEntityMapper;
import com.capysoft.tuevento.modules.event.domain.model.EventLayout;
import com.capysoft.tuevento.modules.event.domain.repository.EventLayoutRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class EventLayoutRepositoryImpl implements EventLayoutRepository {

    private final EventLayoutJpaRepository jpaRepository;
    private final EventLayoutEntityMapper mapper;

    @Override
    public EventLayout save(EventLayout layout) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(layout)));
    }

    @Override
    public Optional<EventLayout> findByEventId(Long eventId) {
        return jpaRepository.findByEventId(eventId).map(mapper::toDomain);
    }
}
