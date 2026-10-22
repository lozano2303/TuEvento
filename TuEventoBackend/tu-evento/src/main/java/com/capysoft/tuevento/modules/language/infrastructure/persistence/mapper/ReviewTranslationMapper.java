package com.capysoft.tuevento.modules.language.infrastructure.persistence.mapper;

import com.capysoft.tuevento.modules.language.domain.model.ReviewTranslation;
import com.capysoft.tuevento.modules.language.infrastructure.persistence.entity.ReviewTranslationEntity;
import com.capysoft.tuevento.modules.language.infrastructure.persistence.entity.LanguageEntity;
import org.springframework.stereotype.Component;

@Component
public class ReviewTranslationMapper {

    public ReviewTranslation toDomain(ReviewTranslationEntity entity) {
        if (entity == null) {
            return null;
        }

        ReviewTranslation domain = new ReviewTranslation();
        domain.setTranslationId(entity.getTranslationId());
        domain.setReviewId(entity.getReviewId());
        domain.setLanguageId(entity.getLanguageId());
        domain.setTranslatedComment(entity.getTranslatedComment());
        domain.setSource(entity.getSource());
        domain.setStatus(entity.getStatus());

        return domain;
    }

    public ReviewTranslationEntity toEntity(ReviewTranslation domain) {
        if (domain == null) {
            return null;
        }

        ReviewTranslationEntity entity = new ReviewTranslationEntity();
        entity.setTranslationId(domain.getTranslationId());
        entity.setReviewId(domain.getReviewId());
        entity.setLanguageId(domain.getLanguageId());
        
        entity.setTranslatedComment(domain.getTranslatedComment());
        entity.setSource(domain.getSource());
        entity.setStatus(domain.getStatus());

        return entity;
    }
}