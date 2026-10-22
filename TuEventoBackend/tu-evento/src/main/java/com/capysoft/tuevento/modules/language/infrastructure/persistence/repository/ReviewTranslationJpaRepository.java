package com.capysoft.tuevento.modules.language.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.language.infrastructure.persistence.entity.ReviewTranslationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewTranslationJpaRepository extends JpaRepository<ReviewTranslationEntity, Integer> {
    
    List<ReviewTranslationEntity> findByReviewId(Integer reviewId);
    
    Optional<ReviewTranslationEntity> findByReviewIdAndLanguageId(Integer reviewId, Integer languageId);
    
    List<ReviewTranslationEntity> findByLanguageId(Integer languageId);
}