package com.capysoft.tuevento.modules.theme.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.theme.infrastructure.persistence.entity.ThemeTranslationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ThemeTranslationJpaRepository extends JpaRepository<ThemeTranslationEntity, Integer> {

    Optional<ThemeTranslationEntity> findByThemeIdAndLanguageId(Integer themeId, Integer languageId);
}
