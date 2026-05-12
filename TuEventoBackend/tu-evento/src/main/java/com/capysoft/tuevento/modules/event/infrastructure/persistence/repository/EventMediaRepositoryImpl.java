package com.capysoft.tuevento.modules.event.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.event.application.mapper.EventMediaEntityMapper;
import com.capysoft.tuevento.modules.event.domain.model.EventMedia;
import com.capysoft.tuevento.modules.event.domain.repository.EventMediaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class EventMediaRepositoryImpl implements EventMediaRepository {

    private final EventMediaJpaRepository jpaRepository;
    private final EventMediaEntityMapper mapper;

    @Override
    public EventMedia save(EventMedia media) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(media)));
    }

    @Override
    public List<EventMedia> findByEventId(Long eventId) {
        return jpaRepository.findByEventId(eventId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<EventMedia> findById(Long mediaId) {
        return jpaRepository.findById(mediaId).map(mapper::toDomain);
    }

    @Override
    public void delete(Long mediaId) {
        jpaRepository.deleteById(mediaId);
    }
}
