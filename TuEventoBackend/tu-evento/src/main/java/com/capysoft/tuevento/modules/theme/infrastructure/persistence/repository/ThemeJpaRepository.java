package com.capysoft.tuevento.modules.theme.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.theme.infrastructure.persistence.entity.ThemeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ThemeJpaRepository extends JpaRepository<ThemeEntity, Integer> {

    Optional<ThemeEntity> findByName(String name);
}
