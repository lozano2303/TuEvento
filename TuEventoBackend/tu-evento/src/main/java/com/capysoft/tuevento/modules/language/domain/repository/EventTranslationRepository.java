package com.capysoft.tuevento.modules.language.domain.repository;

import com.capysoft.tuevento.modules.language.domain.model.EventTranslation;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de dominio para EventTranslation.
 */
public interface EventTranslationRepository {

    /**
     * Busca una traducción específica por evento y idioma.
     */
    Optional<EventTranslation> findByEventAndLanguage(Integer eventId, Integer languageId);

    /**
     * Busca todas las traducciones de un evento.
     */
    List<EventTranslation> findByEvent(Integer eventId);

    /**
     * Busca todas las traducciones en un idioma específico.
     */
    List<EventTranslation> findByLanguage(Integer languageId);

    /**
     * Busca traducciones por estado.
     */
    List<EventTranslation> findByStatus(TranslationStatus status);

    /**
     * Busca traducciones por evento y estado.
     */
    List<EventTranslation> findByEventAndStatus(Integer eventId, TranslationStatus status);

    /**
     * Guarda una traducción.
     */
    EventTranslation save(EventTranslation translation);

    /**
     * Guarda múltiples traducciones.
     */
    List<EventTranslation> saveAll(List<EventTranslation> translations);

    /**
     * Elimina una traducción.
     */
    void delete(EventTranslation translation);

    /**
     * Elimina todas las traducciones de un evento.
     */
    void deleteByEvent(Integer eventId);

    /**
     * Verifica si existe una traducción para evento y idioma específicos.
     */
    boolean existsByEventAndLanguage(Integer eventId, Integer languageId);
}