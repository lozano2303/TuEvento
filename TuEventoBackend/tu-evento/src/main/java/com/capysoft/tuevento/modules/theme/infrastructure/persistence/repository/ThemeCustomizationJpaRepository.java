package com.capysoft.tuevento.modules.theme.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.theme.infrastructure.persistence.entity.ThemeCustomizationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ThemeCustomizationJpaRepository extends JpaRepository<ThemeCustomizationEntity, Integer> {

    List<ThemeCustomizationEntity> findByUserThemeId(Integer userThemeId);

    Optional<ThemeCustomizationEntity> findByUserThemeIdAndProperty(Integer userThemeId, String property);

    void deleteByUserThemeIdAndProperty(Integer userThemeId, String property);
}
