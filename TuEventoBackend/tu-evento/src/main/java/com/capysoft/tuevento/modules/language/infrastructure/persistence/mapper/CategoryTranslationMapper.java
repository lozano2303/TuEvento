package com.capysoft.tuevento.modules.language.infrastructure.persistence.mapper;

import com.capysoft.tuevento.modules.language.domain.model.CategoryTranslation;
import com.capysoft.tuevento.modules.language.infrastructure.persistence.entity.CategoryTranslationEntity;
import com.capysoft.tuevento.modules.language.infrastructure.persistence.entity.LanguageEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper entre CategoryTranslation (dominio) y CategoryTranslationEntity (persistencia).
 */
@Component
public class CategoryTranslationMapper {

    /**
     * Convierte de entidad JPA a modelo de dominio.
     */
    public CategoryTranslation toDomain(CategoryTranslationEntity entity) {
        if (entity == null) {
            return null;
        }

        return CategoryTranslation.builder()
                .translationId(entity.getTranslationId())
                .categoryId(entity.getCategoryId())
                .languageId(entity.getLanguageId())
                .translatedName(entity.getTranslatedName())
                .translatedDescription(entity.getTranslatedDescription())
                .source(entity.getSource())
                .status(entity.getStatus())
                .build();
    }

    /**
     * Convierte de modelo de dominio a entidad JPA.
     */
    public CategoryTranslationEntity toEntity(CategoryTranslation domain) {
        if (domain == null) {
            return null;
        }

        return CategoryTranslationEntity.builder()
                .translationId(domain.getTranslationId())
                .categoryId(domain.getCategoryId())
                .languageId(domain.getLanguageId())
                .translatedName(domain.getTranslatedName())
                .translatedDescription(domain.getTranslatedDescription())
                .source(domain.getSource())
                .status(domain.getStatus())
                .build();
    }

    /**
     * Actualiza una entidad existente con datos del dominio.
     */
    public void updateEntity(CategoryTranslationEntity entity, CategoryTranslation domain) {
        if (entity == null || domain == null) {
            return;
        }

        entity.setTranslatedName(domain.getTranslatedName());
        entity.setTranslatedDescription(domain.getTranslatedDescription());
        entity.setSource(domain.getSource());
        entity.setStatus(domain.getStatus());
    }
}