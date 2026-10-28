package com.capysoft.identity.theme.infrastructure.persistence.repository;

import com.capysoft.identity.theme.infrastructure.persistence.entity.ThemeLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ThemeLogJpaRepository extends JpaRepository<ThemeLogEntity, Integer> {

    List<ThemeLogEntity> findByUserThemeId(Integer userThemeId);
}
