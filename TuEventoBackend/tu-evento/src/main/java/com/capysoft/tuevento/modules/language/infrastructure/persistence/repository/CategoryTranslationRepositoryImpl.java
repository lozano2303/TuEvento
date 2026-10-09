package com.capysoft.tuevento.modules.language.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.language.domain.model.CategoryTranslation;
import com.capysoft.tuevento.modules.language.domain.repository.CategoryTranslationRepository;
import com.capysoft.tuevento.modules.language.infrastructure.persistence.entity.CategoryTranslationEntity;
import com.capysoft.tuevento.modules.language.infrastructure.persistence.entity.LanguageEntity;
import com.capysoft.tuevento.modules.language.infrastructure.persistence.repository.CategoryTranslationJpaRepository;
import com.capysoft.tuevento.modules.language.infrastructure.persistence.mapper.CategoryTranslationMapper;
import com.capysoft.tuevento.modules.language.infrastructure.persistence.repository.LanguageJpaRepository;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Implementación del repositorio de dominio para CategoryTranslation.
 */
@Repository
@RequiredArgsConstructor
public class CategoryTranslationRepositoryImpl implements CategoryTranslationRepository {

    private final CategoryTranslationJpaRepository jpaRepository;
    private final LanguageJpaRepository languageJpaRepository;
    private final CategoryTranslationMapper mapper;

    @Override
    public Optional<CategoryTranslation> findByCategoryAndLanguage(Integer categoryId, Long languageId) {
        return jpaRepository.findByCategoryIdAndLanguageId(categoryId, languageId.intValue())
                .map(mapper::toDomain);
    }

    @Override
    public List<CategoryTranslation> findByCategory(Integer categoryId) {
        return jpaRepository.findByCategoryId(categoryId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<CategoryTranslation> findByLanguage(Long languageId) {
        return jpaRepository.findByLanguageId(languageId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<CategoryTranslation> findByStatus(TranslationStatus status) {
        return jpaRepository.findByStatus(status.name().toLowerCase())
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<CategoryTranslation> findByCategoryAndStatus(Integer categoryId, TranslationStatus status) {
        return jpaRepository.findByCategoryIdAndStatus(categoryId, status.name().toLowerCase())
                .stream()
                .map(mapper::toDomain)
                .toList();
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

        CategoryTranslationEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    @Transactional
    public List<CategoryTranslation> saveAll(List<CategoryTranslation> translations) {
        return translations.stream()
                .map(this::save)
                .toList();
    }

    @Override
    @Transactional
    public void delete(CategoryTranslation translation) {
        if (translation.getTranslationId() != null) {
            jpaRepository.deleteById(translation.getTranslationId());
        }
    }

    @Override
    @Transactional
    public void deleteByCategory(Integer categoryId) {
        jpaRepository.deleteByCategoryId(categoryId);
    }

    @Override
    public boolean existsByCategoryAndLanguage(Integer categoryId, Long languageId) {
        return jpaRepository.existsByCategoryIdAndLanguageId(categoryId, languageId);
    }
}