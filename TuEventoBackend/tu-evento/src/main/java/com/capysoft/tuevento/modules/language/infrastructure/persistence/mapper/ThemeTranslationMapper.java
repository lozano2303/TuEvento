package com.capysoft.tuevento.modules.language.infrastructure.persistence.mapper;

import com.capysoft.tuevento.modules.language.domain.model.ThemeTranslation;
import com.capysoft.tuevento.modules.language.infrastructure.persistence.entity.ThemeTranslationEntity;
import com.capysoft.tuevento.modules.language.infrastructure.persistence.entity.LanguageEntity;
import org.springframework.stereotype.Component;

@Component
public class ThemeTranslationMapper {

    public ThemeTranslation toDomain(ThemeTranslationEntity entity) {
        if (entity == null) {
            return null;
        }

        ThemeTranslation domain = new ThemeTranslation();
        domain.setTranslationId(entity.getTranslationId());
        domain.setThemeId(entity.getThemeId()); // Both are Integer now
        domain.setLanguageId(entity.getLanguage().getLanguageId());
        domain.setTranslatedName(entity.getTranslatedName());
        domain.setTranslatedDescription(entity.getTranslatedDescription());
        domain.setSource(entity.getSource());
        domain.setStatus(entity.getStatus());

        return domain;
    }

    public ThemeTranslationEntity toEntity(ThemeTranslation domain) {
        if (domain == null) {
            return null;
        }

        ThemeTranslationEntity entity = new ThemeTranslationEntity();
        entity.setTranslationId(domain.getTranslationId());
        entity.setThemeId(domain.getThemeId()); // Both are Integer now
        
        // Create a reference to LanguageEntity - will be managed by repository
        LanguageEntity languageEntity = new LanguageEntity();
        languageEntity.setLanguageId(domain.getLanguageId());
        entity.setLanguage(languageEntity);
        
        entity.setTranslatedName(domain.getTranslatedName());
        entity.setTranslatedDescription(domain.getTranslatedDescription());
        entity.setSource(domain.getSource());
        entity.setStatus(domain.getStatus());

        return entity;
    }
}