package com.capysoft.tuevento.modules.theme.infrastructure.persistence.mapper;

import com.capysoft.tuevento.modules.theme.domain.model.ThemeTranslation;
import com.capysoft.tuevento.modules.theme.infrastructure.persistence.entity.ThemeTranslationEntity;
import org.springframework.stereotype.Component;

@Component
public class ThemeTranslationMapper {

    public ThemeTranslation toDomain(ThemeTranslationEntity entity) {
        ThemeTranslation domain = new ThemeTranslation();
        domain.setTranslationId(entity.getTranslationId());
        domain.setThemeId(entity.getThemeId());
        domain.setLanguageId(entity.getLanguageId());
        domain.setTranslatedName(entity.getTranslatedName());
        domain.setTranslatedDescription(entity.getTranslatedDescription());
        domain.setStatus(entity.getStatus());
        return domain;
    }
}
