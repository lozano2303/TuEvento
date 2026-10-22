package com.capysoft.tuevento.modules.language.infrastructure.persistence.mapper;

import com.capysoft.tuevento.modules.language.domain.model.EventRatingTranslation;
import com.capysoft.tuevento.modules.language.infrastructure.persistence.entity.EventRatingTranslationEntity;
import com.capysoft.tuevento.modules.language.infrastructure.persistence.entity.LanguageEntity;
import org.springframework.stereotype.Component;

@Component
public class EventRatingTranslationMapper {

    public EventRatingTranslation toDomain(EventRatingTranslationEntity entity) {
        if (entity == null) {
            return null;
        }

        EventRatingTranslation domain = new EventRatingTranslation();
        domain.setTranslationId(entity.getTranslationId());
        domain.setRatingId(entity.getRatingId());
        domain.setLanguageId(entity.getLanguageId());
        domain.setTranslatedComment(entity.getTranslatedComment());
        domain.setStatus(entity.getStatus());

        return domain;
    }

    public EventRatingTranslationEntity toEntity(EventRatingTranslation domain) {
        if (domain == null) {
            return null;
        }

        EventRatingTranslationEntity entity = new EventRatingTranslationEntity();
        entity.setTranslationId(domain.getTranslationId());
        entity.setRatingId(domain.getRatingId());
        entity.setLanguageId(domain.getLanguageId());
        
        entity.setTranslatedComment(domain.getTranslatedComment());
        entity.setStatus(domain.getStatus());

        return entity;
    }
}