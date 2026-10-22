package com.capysoft.tuevento.modules.language.domain.repository;

import com.capysoft.tuevento.modules.language.domain.model.ActionTranslation;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de dominio para ActionTranslation.
 */
public interface ActionTranslationRepository {

    /**
     * Busca una traducción específica por action y idioma.
     */
    Optional<ActionTranslation> findByActionAndLanguage(String action, Integer languageId);

    /**
     * Busca todas las traducciones de un action.
     */
    List<ActionTranslation> findByAction(String action);

    /**
     * Busca todas las traducciones en un idioma específico.
     */
    List<ActionTranslation> findByLanguage(Integer languageId);

    /**
     * Busca traducciones por estado.
     */
    List<ActionTranslation> findByStatus(TranslationStatus status);

    /**
     * Busca traducciones por action y estado.
     */
    List<ActionTranslation> findByActionAndStatus(String action, TranslationStatus status);

    /**
     * Guarda una traducción.
     */
    ActionTranslation save(ActionTranslation translation);

    /**
     * Guarda múltiples traducciones.
     */
    List<ActionTranslation> saveAll(List<ActionTranslation> translations);

    /**
     * Elimina una traducción.
     */
    void delete(ActionTranslation translation);

    /**
     * Elimina todas las traducciones de un action.
     */
    void deleteByAction(String action);

    /**
     * Verifica si existe una traducción para action y idioma específicos.
     */
    boolean existsByActionAndLanguage(String action, Integer languageId);
}