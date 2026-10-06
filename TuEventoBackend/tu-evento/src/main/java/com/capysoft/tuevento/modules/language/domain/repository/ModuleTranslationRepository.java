package com.capysoft.tuevento.modules.language.domain.repository;

import com.capysoft.tuevento.modules.language.domain.model.ModuleTranslation;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de dominio para ModuleTranslation.
 */
public interface ModuleTranslationRepository {

    /**
     * Busca una traducción específica por module y idioma.
     */
    Optional<ModuleTranslation> findByModuleAndLanguage(String module, Long languageId);

    /**
     * Busca todas las traducciones de un module.
     */
    List<ModuleTranslation> findByModule(String module);

    /**
     * Busca todas las traducciones en un idioma específico.
     */
    List<ModuleTranslation> findByLanguage(Long languageId);

    /**
     * Busca traducciones por estado.
     */
    List<ModuleTranslation> findByStatus(TranslationStatus status);

    /**
     * Busca traducciones por module y estado.
     */
    List<ModuleTranslation> findByModuleAndStatus(String module, TranslationStatus status);

    /**
     * Guarda una traducción.
     */
    ModuleTranslation save(ModuleTranslation translation);

    /**
     * Guarda múltiples traducciones.
     */
    List<ModuleTranslation> saveAll(List<ModuleTranslation> translations);

    /**
     * Elimina una traducción.
     */
    void delete(ModuleTranslation translation);

    /**
     * Elimina todas las traducciones de un module.
     */
    void deleteByModule(String module);

    /**
     * Verifica si existe una traducción para module y idioma específicos.
     */
    boolean existsByModuleAndLanguage(String module, Long languageId);
}