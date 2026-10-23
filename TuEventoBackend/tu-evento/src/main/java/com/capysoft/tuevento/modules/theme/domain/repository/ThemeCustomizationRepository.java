package com.capysoft.tuevento.modules.theme.domain.repository;

import com.capysoft.tuevento.modules.theme.domain.model.ThemeCustomization;

import java.util.List;
import java.util.Optional;

public interface ThemeCustomizationRepository {

    List<ThemeCustomization> findByUserThemeId(Integer userThemeId);
    Optional<ThemeCustomization> findByUserThemeIdAndProperty(Integer userThemeId, String property);
    ThemeCustomization save(ThemeCustomization customization);
    void deleteByUserThemeIdAndProperty(Integer userThemeId, String property);
}
