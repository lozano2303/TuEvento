package com.capysoft.tuevento.modules.section.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.section.infrastructure.persistence.entity.SectionTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SectionTypeJpaRepository extends JpaRepository<SectionTypeEntity, Integer> {
    boolean existsByName(String name);
}
