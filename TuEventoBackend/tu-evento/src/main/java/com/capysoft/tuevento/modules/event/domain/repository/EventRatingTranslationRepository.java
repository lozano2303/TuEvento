package com.capysoft.tuevento.modules.event.domain.repository;

import com.capysoft.tuevento.modules.event.domain.model.EventRatingTranslation;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de dominio para las traducciones de comentarios de eventos.
 */
public interface EventRatingTranslationRepository {

    Optional<EventRatingTranslation> findByRatingAndLanguage(Integer ratingId, Integer languageId);

    /** Traducciones de varios comentarios en un estado, en cualquier idioma. */
    List<EventRatingTranslation> findByRatingAndStatus(List<Integer> ratingIds, TranslationStatus status);

    EventRatingTranslation save(EventRatingTranslation translation);

    void delete(EventRatingTranslation translation);

    /** Borra todas las traducciones de un comentario (al crearlo, editarlo o borrarlo). */
    void deleteByRating(Integer ratingId);
}
