package com.capysoft.tuevento.modules.event.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.event.infrastructure.persistence.entity.EventRatingTranslationEntity;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRatingTranslationJpaRepository extends JpaRepository<EventRatingTranslationEntity, Integer> {

    List<EventRatingTranslationEntity> findByRatingId(Integer ratingId);

    Optional<EventRatingTranslationEntity> findByRatingIdAndLanguageId(Integer ratingId, Integer languageId);

    List<EventRatingTranslationEntity> findByRatingIdInAndStatus(Collection<Integer> ratingIds, TranslationStatus status);
}
