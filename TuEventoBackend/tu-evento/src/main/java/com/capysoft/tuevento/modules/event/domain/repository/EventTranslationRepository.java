package com.capysoft.tuevento.modules.event.domain.repository;

import com.capysoft.tuevento.modules.event.domain.model.EventTranslation;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de dominio para las traducciones de eventos.
 */
public interface EventTranslationRepository {

    Optional<EventTranslation> findByEventAndLanguage(Integer eventId, Integer languageId);

    /** Todas las traducciones de un evento, en cualquier idioma. */
    List<EventTranslation> findByEvent(Integer eventId);

    /** Guarda una traducción: la crea si no tiene id o actualiza la existente. */
    EventTranslation save(EventTranslation translation);

    void delete(EventTranslation translation);

    /** Borra todas las traducciones de un evento (cuando cambia su nombre o descripción). */
    void deleteByEvent(Integer eventId);
}
