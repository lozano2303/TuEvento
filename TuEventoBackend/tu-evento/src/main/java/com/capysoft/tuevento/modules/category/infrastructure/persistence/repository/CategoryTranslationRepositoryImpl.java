package com.capysoft.tuevento.modules.category.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.category.domain.model.CategoryTranslation;
import com.capysoft.tuevento.modules.category.domain.repository.CategoryTranslationRepository;
import com.capysoft.tuevento.modules.category.infrastructure.persistence.entity.CategoryTranslationEntity;
import com.capysoft.tuevento.modules.category.infrastructure.persistence.mapper.CategoryTranslationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Implementación del repositorio de dominio para CategoryTranslation.
 */
@Repository
@RequiredArgsConstructor
public class CategoryTranslationRepositoryImpl implements CategoryTranslationRepository {

    private final CategoryTranslationJpaRepository jpaRepository;
    private final CategoryTranslationMapper mapper;

    @Override
    public Optional<CategoryTranslation> findByCategoryAndLanguage(Integer categoryId, Integer languageId) {
        return jpaRepository.findByCategoryIdAndLanguageId(categoryId, languageId)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional
    public CategoryTranslation save(CategoryTranslation translation) {
        CategoryTranslationEntity entity;
        if (translation.getTranslationId() != null) {
            // Actualizar entidad existente
            entity = jpaRepository.findById(translation.getTranslationId())
                    .orElseThrow(() -> new IllegalArgumentException("Translation not found: " + translation.getTranslationId()));
            mapper.updateEntity(entity, translation);
        } else {
            // Crear nueva entidad
            entity = mapper.toEntity(translation);
        }
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    @Transactional
    public void delete(CategoryTranslation translation) {
        if (translation.getTranslationId() != null) {
            jpaRepository.deleteById(translation.getTranslationId());
        }
    }
}
