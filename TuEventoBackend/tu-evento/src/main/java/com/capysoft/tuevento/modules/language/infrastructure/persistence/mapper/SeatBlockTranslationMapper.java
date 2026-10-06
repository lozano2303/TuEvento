package com.capysoft.tuevento.modules.language.infrastructure.persistence.mapper;

import com.capysoft.tuevento.modules.language.domain.model.SeatBlockTranslation;
import com.capysoft.tuevento.modules.language.infrastructure.persistence.entity.SeatBlockTranslationEntity;
import com.capysoft.tuevento.modules.language.infrastructure.persistence.entity.LanguageEntity;
import org.springframework.stereotype.Component;

@Component
public class SeatBlockTranslationMapper {

    public SeatBlockTranslation toDomain(SeatBlockTranslationEntity entity) {
        if (entity == null) {
            return null;
        }

        SeatBlockTranslation domain = new SeatBlockTranslation();
        domain.setTranslationId(entity.getTranslationId());
        domain.setSeatBlockId(entity.getSeatBlockId());
        domain.setLanguageId(entity.getLanguage().getLanguageId());
        domain.setTranslatedName(entity.getTranslatedName());
        domain.setSource(entity.getSource());
        domain.setStatus(entity.getStatus());

        return domain;
    }

    public SeatBlockTranslationEntity toEntity(SeatBlockTranslation domain) {
        if (domain == null) {
            return null;
        }

        SeatBlockTranslationEntity entity = new SeatBlockTranslationEntity();
        entity.setTranslationId(domain.getTranslationId());
        entity.setSeatBlockId(domain.getSeatBlockId());
        
        // Create a reference to LanguageEntity - will be managed by repository
        LanguageEntity languageEntity = new LanguageEntity();
        languageEntity.setLanguageId(domain.getLanguageId());
        entity.setLanguage(languageEntity);
        
        entity.setTranslatedName(domain.getTranslatedName());
        entity.setSource(domain.getSource());
        entity.setStatus(domain.getStatus());

        return entity;
    }
}