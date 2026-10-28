package com.capysoft.tuevento.modules.category.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.category.infrastructure.persistence.entity.CategoryTranslationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryTranslationJpaRepository extends JpaRepository<CategoryTranslationEntity, Integer> {

    Optional<CategoryTranslationEntity> findByCategoryIdAndLanguageId(Integer categoryId, Integer languageId);
}
