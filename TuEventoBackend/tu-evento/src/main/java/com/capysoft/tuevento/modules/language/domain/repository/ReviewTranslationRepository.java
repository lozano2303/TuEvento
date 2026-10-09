package com.capysoft.tuevento.modules.language.domain.repository;

import com.capysoft.tuevento.modules.language.domain.model.ReviewTranslation;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de dominio para ReviewTranslation.
 */
public interface ReviewTranslationRepository {

    /**
     * Busca una traducción específica por review y idioma.
     */
    Optional<ReviewTranslation> findByReviewAndLanguage(Long reviewId, Integer languageId);

    /**
     * Busca todas las traducciones de un review.
     */
    List<ReviewTranslation> findByReview(Long reviewId);

    /**
     * Busca todas las traducciones en un idioma específico.
     */
    List<ReviewTranslation> findByLanguage(Integer languageId);

    /**
     * Busca traducciones por estado.
     */
    List<ReviewTranslation> findByStatus(TranslationStatus status);

    /**
     * Busca traducciones por review y estado.
     */
    List<ReviewTranslation> findByReviewAndStatus(Long reviewId, TranslationStatus status);

    /**
     * Guarda una traducción.
     */
    ReviewTranslation save(ReviewTranslation translation);

    /**
     * Guarda múltiples traducciones.
     */
    List<ReviewTranslation> saveAll(List<ReviewTranslation> translations);

    /**
     * Elimina una traducción.
     */
    void delete(ReviewTranslation translation);

    /**
     * Elimina todas las traducciones de un review.
     */
    void deleteByReview(Long reviewId);

    /**
     * Verifica si existe una traducción para review y idioma específicos.
     */
    boolean existsByReviewAndLanguage(Long reviewId, Integer languageId);
}