package com.capysoft.tuevento.modules.language.infrastructure.persistence.mapper;

import com.capysoft.tuevento.modules.language.domain.model.ProfileTranslation;
import com.capysoft.tuevento.modules.language.infrastructure.persistence.entity.ProfileTranslationEntity;
import com.capysoft.tuevento.modules.language.infrastructure.persistence.entity.LanguageEntity;
import org.springframework.stereotype.Component;

@Component
public class ProfileTranslationMapper {

    public ProfileTranslation toDomain(ProfileTranslationEntity entity) {
        if (entity == null) {
            return null;
        }

        ProfileTranslation domain = new ProfileTranslation();
        domain.setTranslationId(entity.getTranslationId());
        domain.setProfileId(entity.getProfileId());
        domain.setLanguageId(entity.getLanguageId());
        domain.setTranslatedBio(entity.getTranslatedBio());
        domain.setSource(entity.getSource());
        domain.setStatus(entity.getStatus());

        return domain;
    }

    public ProfileTranslationEntity toEntity(ProfileTranslation domain) {
        if (domain == null) {
            return null;
        }

        ProfileTranslationEntity entity = new ProfileTranslationEntity();
        entity.setTranslationId(domain.getTranslationId());
        entity.setProfileId(domain.getProfileId());
        entity.setLanguageId(domain.getLanguageId());
        
        entity.setTranslatedBio(domain.getTranslatedBio());
        entity.setSource(domain.getSource());
        entity.setStatus(domain.getStatus());

        return entity;
    }
}