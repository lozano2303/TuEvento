package com.capysoft.tuevento.modules.language.domain.repository;

import com.capysoft.tuevento.modules.language.domain.model.ThemeTranslation;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de dominio para ThemeTranslation.
 */
public interface ThemeTranslationRepository {

    /**
     * Busca una traducción específica por tema y idioma.
     */
    Optional<ThemeTranslation> findByThemeAndLanguage(Integer themeId, Integer languageId);

    /**
     * Busca todas las traducciones de un tema.
     */
    List<ThemeTranslation> findByTheme(Integer themeId);

    /**
     * Busca todas las traducciones en un idioma específico.
     */
    List<ThemeTranslation> findByLanguage(Integer languageId);

    /**
     * Busca traducciones por estado.
     */
    List<ThemeTranslation> findByStatus(TranslationStatus status);

    /**
     * Busca traducciones por tema y estado.
     */
    List<ThemeTranslation> findByThemeAndStatus(Integer themeId, TranslationStatus status);

    /**
     * Guarda una traducción.
     */
    ThemeTranslation save(ThemeTranslation translation);

    /**
     * Guarda múltiples traducciones.
     */
    List<ThemeTranslation> saveAll(List<ThemeTranslation> translations);

    /**
     * Elimina una traducción.
     */
    void delete(ThemeTranslation translation);

    /**
     * Elimina todas las traducciones de un tema.
     */
    void deleteByTheme(Integer themeId);

    /**
     * Verifica si existe una traducción para tema y idioma específicos.
     */
    boolean existsByThemeAndLanguage(Integer themeId, Integer languageId);
}