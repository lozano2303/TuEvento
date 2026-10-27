package com.capysoft.tuevento.modules.section.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.section.infrastructure.persistence.entity.SectionTypeTranslationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SectionTypeTranslationJpaRepository extends JpaRepository<SectionTypeTranslationEntity, Integer> {

    Optional<SectionTypeTranslationEntity> findBySectionTypeIdAndLanguageId(Integer sectionTypeId, Integer languageId);
}
