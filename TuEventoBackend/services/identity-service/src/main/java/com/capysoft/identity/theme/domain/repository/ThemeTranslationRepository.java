package com.capysoft.identity.theme.domain.repository;

import com.capysoft.identity.theme.domain.model.ThemeTranslation;

import java.util.Optional;

/**
 * Repositorio de dominio para ThemeTranslation.
 */
public interface ThemeTranslationRepository {

    /**
     * Busca una traducción específica por tema y idioma.
     */
    Optional<ThemeTranslation> findByThemeAndLanguage(Integer themeId, Integer languageId);
}
