package com.capysoft.tuevento.modules.language.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.language.infrastructure.persistence.entity.SectionTypeTranslationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SectionTypeTranslationJpaRepository extends JpaRepository<SectionTypeTranslationEntity, Integer> {
    
    List<SectionTypeTranslationEntity> findBySectionTypeId(Integer sectionTypeId);
    
    Optional<SectionTypeTranslationEntity> findBySectionTypeIdAndLanguageId(Integer sectionTypeId, Integer languageId);
    
    List<SectionTypeTranslationEntity> findByLanguageId(Integer languageId);
}