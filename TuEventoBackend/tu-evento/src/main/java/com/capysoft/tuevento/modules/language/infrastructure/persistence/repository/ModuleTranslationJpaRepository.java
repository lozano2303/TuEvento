package com.capysoft.tuevento.modules.language.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.language.infrastructure.persistence.entity.ModuleTranslationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ModuleTranslationJpaRepository extends JpaRepository<ModuleTranslationEntity, Integer> {
    
    List<ModuleTranslationEntity> findByModule(String module);
    
    Optional<ModuleTranslationEntity> findByModuleAndLanguageId(String module, Integer languageId);
    
    List<ModuleTranslationEntity> findByLanguageId(Integer languageId);
}