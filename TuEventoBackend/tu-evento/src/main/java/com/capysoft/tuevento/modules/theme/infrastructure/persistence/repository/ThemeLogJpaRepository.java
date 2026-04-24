package com.capysoft.tuevento.modules.theme.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.theme.infrastructure.persistence.entity.ThemeLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ThemeLogJpaRepository extends JpaRepository<ThemeLogEntity, Integer> {

    List<ThemeLogEntity> findByUserThemeId(Integer userThemeId);
}
