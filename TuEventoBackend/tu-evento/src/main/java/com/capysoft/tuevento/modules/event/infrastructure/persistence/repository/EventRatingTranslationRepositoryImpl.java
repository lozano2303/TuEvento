package com.capysoft.tuevento.modules.event.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.event.domain.model.EventRatingTranslation;
import com.capysoft.tuevento.modules.event.domain.repository.EventRatingTranslationRepository;
import com.capysoft.tuevento.modules.event.infrastructure.persistence.mapper.EventRatingTranslationMapper;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class EventRatingTranslationRepositoryImpl implements EventRatingTranslationRepository {

    private final EventRatingTranslationJpaRepository jpaRepository;
    private final EventRatingTranslationMapper mapper;

    @Override
    public Optional<EventRatingTranslation> findByRatingAndLanguage(Integer ratingId, Integer languageId) {
        return jpaRepository.findByRatingIdAndLanguageId(ratingId, languageId).map(mapper::toDomain);
    }

    @Override
    public List<EventRatingTranslation> findByRatingAndStatus(List<Integer> ratingIds, TranslationStatus status) {
        if (ratingIds.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findByRatingIdInAndStatus(ratingIds, status).stream().map(mapper::toDomain).toList();
    }

    @Override
    public EventRatingTranslation save(EventRatingTranslation translation) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(translation)));
    }

    @Override
    public void delete(EventRatingTranslation translation) {
        jpaRepository.delete(mapper.toEntity(translation));
    }

    @Override
    public void deleteByRating(Integer ratingId) {
        jpaRepository.deleteAll(jpaRepository.findByRatingId(ratingId));
    }
}
