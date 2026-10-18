package com.capysoft.tuevento.modules.language.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.language.domain.model.EventRatingTranslation;
import com.capysoft.tuevento.modules.language.domain.repository.EventRatingTranslationRepository;
import com.capysoft.tuevento.modules.language.infrastructure.persistence.mapper.EventRatingTranslationMapper;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class EventRatingTranslationRepositoryImpl implements EventRatingTranslationRepository {

    private final EventRatingTranslationJpaRepository jpaRepository;
    private final EventRatingTranslationMapper mapper;

    @Override
    public Optional<EventRatingTranslation> findByRatingAndLanguage(Integer ratingId, Integer languageId) {
        return jpaRepository.findByRatingIdAndLanguageId(ratingId.longValue(), languageId)
                .map(mapper::toDomain);
    }

    @Override
    public List<EventRatingTranslation> findByRating(Integer ratingId) {
        return jpaRepository.findByRatingId(ratingId.longValue())
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<EventRatingTranslation> findByLanguage(Integer languageId) {
        return jpaRepository.findByLanguageId(languageId)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<EventRatingTranslation> findByStatus(TranslationStatus status) {
        return jpaRepository.findAll()
                .stream()
                .map(mapper::toDomain)
                .filter(translation -> status.equals(translation.getStatus()))
                .collect(Collectors.toList());
    }

    @Override
    public List<EventRatingTranslation> findByRatingAndStatus(Integer ratingId, TranslationStatus status) {
        return findByRating(ratingId)
                .stream()
                .filter(translation -> status.equals(translation.getStatus()))
                .collect(Collectors.toList());
    }

    @Override
    public List<EventRatingTranslation> findByRatingAndStatus(List<Integer> ratingIds, TranslationStatus status) {
        if (ratingIds == null || ratingIds.isEmpty()) {
            return List.of();
        }
        List<Long> longRatingIds = ratingIds.stream()
                .map(Integer::longValue)
                .collect(Collectors.toList());
        return jpaRepository.findByRatingIdsAndStatus(longRatingIds, status.name())
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public EventRatingTranslation save(EventRatingTranslation translation) {
        var entity = mapper.toEntity(translation);
        var savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public List<EventRatingTranslation> saveAll(List<EventRatingTranslation> translations) {
        var entities = translations.stream()
                .map(mapper::toEntity)
                .collect(Collectors.toList());
        var savedEntities = jpaRepository.saveAll(entities);
        return savedEntities.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(EventRatingTranslation translation) {
        var entity = mapper.toEntity(translation);
        jpaRepository.delete(entity);
    }

    @Override
    public void deleteByRating(Integer ratingId) {
        var entities = jpaRepository.findByRatingId(ratingId.longValue());
        jpaRepository.deleteAll(entities);
    }

    @Override
    public boolean existsByRatingAndLanguage(Integer ratingId, Integer languageId) {
        return jpaRepository.findByRatingIdAndLanguageId(ratingId.longValue(), languageId).isPresent();
    }
}