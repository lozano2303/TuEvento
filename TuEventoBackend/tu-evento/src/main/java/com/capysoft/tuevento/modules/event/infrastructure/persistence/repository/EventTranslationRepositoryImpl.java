package com.capysoft.tuevento.modules.event.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.event.domain.model.EventTranslation;
import com.capysoft.tuevento.modules.event.domain.repository.EventTranslationRepository;
import com.capysoft.tuevento.modules.event.infrastructure.persistence.entity.EventTranslationEntity;
import com.capysoft.tuevento.modules.event.infrastructure.persistence.mapper.EventTranslationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Implementación del repositorio de dominio para EventTranslation.
 */
@Repository
@RequiredArgsConstructor
public class EventTranslationRepositoryImpl implements EventTranslationRepository {

    private final EventTranslationJpaRepository jpaRepository;
    private final EventTranslationMapper mapper;

    @Override
    public Optional<EventTranslation> findByEventAndLanguage(Integer eventId, Integer languageId) {
        return jpaRepository.findByEventIdAndLanguageId(eventId, languageId).map(mapper::toDomain);
    }

    @Override
    public List<EventTranslation> findByEvent(Integer eventId) {
        return jpaRepository.findByEventId(eventId).stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional
    public EventTranslation save(EventTranslation translation) {
        EventTranslationEntity entity = translation.getTranslationId() == null
                ? mapper.toEntity(translation)
                : jpaRepository.findById(translation.getTranslationId())
                        .map(existing -> {
                            mapper.updateEntity(existing, translation);
                            return existing;
                        })
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Translation not found: " + translation.getTranslationId()));
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    @Transactional
    public void delete(EventTranslation translation) {
        if (translation.getTranslationId() != null) {
            jpaRepository.deleteById(translation.getTranslationId());
        }
    }

    @Override
    @Transactional
    public void deleteByEvent(Integer eventId) {
        jpaRepository.deleteByEventId(eventId);
    }
}
