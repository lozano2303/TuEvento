package com.capysoft.tuevento.modules.language.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.language.domain.model.EventTranslation;
import com.capysoft.tuevento.modules.language.domain.repository.EventTranslationRepository;
import com.capysoft.tuevento.modules.language.infrastructure.persistence.entity.EventTranslationEntity;
import com.capysoft.tuevento.modules.language.infrastructure.persistence.entity.LanguageEntity;
import com.capysoft.tuevento.modules.language.infrastructure.persistence.repository.EventTranslationJpaRepository;
import com.capysoft.tuevento.modules.language.infrastructure.persistence.mapper.EventTranslationMapper;
import com.capysoft.tuevento.modules.language.infrastructure.persistence.repository.LanguageJpaRepository;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
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
    private final LanguageJpaRepository languageJpaRepository;
    private final EventTranslationMapper mapper;

    @Override
    public Optional<EventTranslation> findByEventAndLanguage(Integer eventId, Integer languageId) {
        return jpaRepository.findByEventIdAndLanguageId(eventId.longValue(), languageId)
                .map(mapper::toDomain);
    }

    @Override
    public List<EventTranslation> findByEvent(Integer eventId) {
        return jpaRepository.findByEventId(eventId.longValue())
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<EventTranslation> findByLanguage(Integer languageId) {
        return jpaRepository.findByLanguageId(languageId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<EventTranslation> findByStatus(TranslationStatus status) {
        return jpaRepository.findByStatus(status.name().toLowerCase())
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<EventTranslation> findByEventAndStatus(Integer eventId, TranslationStatus status) {
        return jpaRepository.findByEventIdAndStatus(eventId.longValue(), status.name().toLowerCase())
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public EventTranslation save(EventTranslation translation) {
        EventTranslationEntity entity;
        if (translation.getTranslationId() != null) {
            // Actualizar entidad existente
            entity = jpaRepository.findById(translation.getTranslationId())
                    .orElseThrow(() -> new IllegalArgumentException("Translation not found: " + translation.getTranslationId()));
            mapper.updateEntity(entity, translation);
        } else {
            // Crear nueva entidad
            entity = mapper.toEntity(translation);
        }

        EventTranslationEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    @Transactional
    public List<EventTranslation> saveAll(List<EventTranslation> translations) {
        return translations.stream()
                .map(this::save)
                .toList();
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
        jpaRepository.deleteByEventId(eventId.longValue());
    }

    @Override
    public boolean existsByEventAndLanguage(Integer eventId, Integer languageId) {
        return jpaRepository.existsByEventIdAndLanguageId(eventId.longValue(), languageId);
    }
}