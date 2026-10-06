package com.capysoft.tuevento.modules.language.infrastructure.persistence.mapper;

import com.capysoft.tuevento.modules.language.domain.model.NotificationTranslation;
import com.capysoft.tuevento.modules.language.infrastructure.persistence.entity.NotificationTranslationEntity;
import com.capysoft.tuevento.modules.language.infrastructure.persistence.entity.LanguageEntity;
import org.springframework.stereotype.Component;

@Component
public class NotificationTranslationMapper {

    public NotificationTranslation toDomain(NotificationTranslationEntity entity) {
        if (entity == null) {
            return null;
        }

        NotificationTranslation domain = new NotificationTranslation();
        domain.setTranslationId(entity.getTranslationId());
        domain.setNotificationId(entity.getNotificationId());
        domain.setLanguageId(entity.getLanguage().getLanguageId());
        domain.setTranslatedSubject(entity.getTranslatedSubject());
        domain.setTranslatedBody(entity.getTranslatedBody());
        domain.setSource(entity.getSource());
        domain.setStatus(entity.getStatus());

        return domain;
    }

    public NotificationTranslationEntity toEntity(NotificationTranslation domain) {
        if (domain == null) {
            return null;
        }

        NotificationTranslationEntity entity = new NotificationTranslationEntity();
        entity.setTranslationId(domain.getTranslationId());
        entity.setNotificationId(domain.getNotificationId());
        
        // Create a reference to LanguageEntity - will be managed by repository
        LanguageEntity languageEntity = new LanguageEntity();
        languageEntity.setLanguageId(domain.getLanguageId());
        entity.setLanguage(languageEntity);
        
        entity.setTranslatedSubject(domain.getTranslatedSubject());
        entity.setTranslatedBody(domain.getTranslatedBody());
        entity.setSource(domain.getSource());
        entity.setStatus(domain.getStatus());

        return entity;
    }
}