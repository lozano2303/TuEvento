package com.capysoft.tuevento.modules.language.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.language.domain.model.ReviewTranslation;
import com.capysoft.tuevento.modules.language.domain.repository.ReviewTranslationRepository;
import com.capysoft.tuevento.modules.language.infrastructure.persistence.mapper.ReviewTranslationMapper;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ReviewTranslationRepositoryImpl implements ReviewTranslationRepository {

    private final ReviewTranslationJpaRepository jpaRepository;
    private final ReviewTranslationMapper mapper;

    @Override
    public Optional<ReviewTranslation> findByReviewAndLanguage(Long reviewId, Long languageId) {
        return jpaRepository.findByReviewIdAndLanguageId(reviewId.intValue(), languageId.intValue())
                .map(mapper::toDomain);
    }

    @Override
    public List<ReviewTranslation> findByReview(Long reviewId) {
        return jpaRepository.findByReviewId(reviewId.intValue())
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReviewTranslation> findByLanguage(Long languageId) {
        return jpaRepository.findByLanguageId(languageId.intValue())
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReviewTranslation> findByStatus(TranslationStatus status) {
        return jpaRepository.findAll()
                .stream()
                .map(mapper::toDomain)
                .filter(translation -> status.equals(translation.getStatus()))
                .collect(Collectors.toList());
    }

    @Override
    public List<ReviewTranslation> findByReviewAndStatus(Long reviewId, TranslationStatus status) {
        return findByReview(reviewId)
                .stream()
                .filter(translation -> status.equals(translation.getStatus()))
                .collect(Collectors.toList());
    }

    @Override
    public ReviewTranslation save(ReviewTranslation translation) {
        var entity = mapper.toEntity(translation);
        var savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public List<ReviewTranslation> saveAll(List<ReviewTranslation> translations) {
        var entities = translations.stream()
                .map(mapper::toEntity)
                .collect(Collectors.toList());
        var savedEntities = jpaRepository.saveAll(entities);
        return savedEntities.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(ReviewTranslation translation) {
        var entity = mapper.toEntity(translation);
        jpaRepository.delete(entity);
    }

    @Override
    public void deleteByReview(Long reviewId) {
        var entities = jpaRepository.findByReviewId(reviewId.intValue());
        jpaRepository.deleteAll(entities);
    }

    @Override
    public boolean existsByReviewAndLanguage(Long reviewId, Long languageId) {
        return jpaRepository.findByReviewIdAndLanguageId(reviewId.intValue(), languageId.intValue()).isPresent();
    }
}