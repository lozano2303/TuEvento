package com.capysoft.tuevento.modules.language.domain.repository;

import com.capysoft.tuevento.modules.language.domain.model.Language;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de dominio para Language.
 */
public interface LanguageRepository {

    /**
     * Busca un idioma por su ID.
     */
    Optional<Language> findById(Long languageId);

    /**
     * Busca un idioma por su código.
     */
    Optional<Language> findByCode(String code);

    /**
     * Obtiene el idioma por defecto del sistema.
     */
    Optional<Language> findDefaultLanguage();

    /**
     * Obtiene todos los idiomas activos.
     */
    List<Language> findAllActive();

    /**
     * Obtiene todos los idiomas (activos e inactivos).
     */
    List<Language> findAll();

    /**
     * Guarda un idioma.
     */
    Language save(Language language);

    /**
     * Verifica si existe un idioma con el código dado (case-insensitive).
     */
    boolean existsByCodeIgnoreCase(String code);

    /**
     * Fuerza la sincronización con la base de datos.
     */
    void flush();
}