package com.capysoft.tuevento.modules.notification.infrastructure.persistence.mapper;

import com.capysoft.tuevento.modules.notification.domain.model.NotificationTranslation;
import com.capysoft.tuevento.modules.notification.infrastructure.persistence.entity.NotificationTranslationEntity;
import org.springframework.stereotype.Component;

@Component
public class NotificationTranslationMapper {

    public NotificationTranslation toDomain(NotificationTranslationEntity entity) {
        return NotificationTranslation.builder()
                .translationId(entity.getTranslationId())
                .notificationId(entity.getNotificationId())
                .languageId(entity.getLanguageId())
                .translatedSubject(entity.getTranslatedSubject())
                .translatedBody(entity.getTranslatedBody())
                .source(entity.getSource())
                .status(entity.getStatus())
                .build();
    }

    public NotificationTranslationEntity toEntity(NotificationTranslation domain) {
        return NotificationTranslationEntity.builder()
                .translationId(domain.getTranslationId())
                .notificationId(domain.getNotificationId())
                .languageId(domain.getLanguageId())
                .translatedSubject(domain.getTranslatedSubject())
                .translatedBody(domain.getTranslatedBody())
                .source(domain.getSource())
                .status(domain.getStatus())
                .build();
    }
}
