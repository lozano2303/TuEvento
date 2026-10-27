package com.capysoft.tuevento.modules.theme.domain.repository;

import com.capysoft.tuevento.modules.theme.domain.model.ThemeTranslation;

import java.util.Optional;

/**
 * Repositorio de dominio para ThemeTranslation.
 */
public interface ThemeTranslationRepository {

    Optional<ThemeTranslation> findByThemeAndLanguage(Integer themeId, Integer languageId);
}
