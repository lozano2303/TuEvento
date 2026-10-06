package com.capysoft.tuevento.modules.language.domain.repository;

import com.capysoft.tuevento.modules.language.domain.model.EventRatingTranslation;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de dominio para EventRatingTranslation.
 */
public interface EventRatingTranslationRepository {

    /**
     * Busca una traducción específica por rating y idioma.
     */
    Optional<EventRatingTranslation> findByRatingAndLanguage(Long ratingId, Long languageId);

    /**
     * Busca todas las traducciones de un rating.
     */
    List<EventRatingTranslation> findByRating(Long ratingId);

    /**
     * Busca todas las traducciones en un idioma específico.
     */
    List<EventRatingTranslation> findByLanguage(Long languageId);

    /**
     * Busca traducciones por estado.
     */
    List<EventRatingTranslation> findByStatus(TranslationStatus status);

    /**
     * Busca traducciones por rating y estado.
     */
    List<EventRatingTranslation> findByRatingAndStatus(Long ratingId, TranslationStatus status);

    /**
     * Guarda una traducción.
     */
    EventRatingTranslation save(EventRatingTranslation translation);

    /**
     * Guarda múltiples traducciones.
     */
    List<EventRatingTranslation> saveAll(List<EventRatingTranslation> translations);

    /**
     * Elimina una traducción.
     */
    void delete(EventRatingTranslation translation);

    /**
     * Elimina todas las traducciones de un rating.
     */
    void deleteByRating(Long ratingId);

    /**
     * Verifica si existe una traducción para rating y idioma específicos.
     */
    boolean existsByRatingAndLanguage(Long ratingId, Long languageId);
}