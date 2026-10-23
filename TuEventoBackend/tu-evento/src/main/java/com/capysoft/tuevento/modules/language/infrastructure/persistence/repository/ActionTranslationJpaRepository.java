package com.capysoft.tuevento.modules.language.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.language.infrastructure.persistence.entity.ActionTranslationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ActionTranslationJpaRepository extends JpaRepository<ActionTranslationEntity, Integer> {
    
    List<ActionTranslationEntity> findByAction(String action);
    
    Optional<ActionTranslationEntity> findByActionAndLanguageId(String action, Integer languageId);
    
    List<ActionTranslationEntity> findByLanguageId(Integer languageId);
}