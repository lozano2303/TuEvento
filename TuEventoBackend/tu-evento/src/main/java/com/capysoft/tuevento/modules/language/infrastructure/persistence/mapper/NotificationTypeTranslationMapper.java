package com.capysoft.tuevento.modules.language.infrastructure.persistence.mapper;

import com.capysoft.tuevento.shared.domain.valueobject.TranslationSource;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import com.capysoft.tuevento.modules.language.domain.model.NotificationTypeTranslation;
import com.capysoft.tuevento.modules.language.infrastructure.persistence.entity.NotificationTypeTranslationEntity;
import com.capysoft.tuevento.modules.language.infrastructure.persistence.entity.TranslationStatusEnum;
import org.springframework.stereotype.Component;

@Component
public class NotificationTypeTranslationMapper {

    public NotificationTypeTranslationEntity toEntity(NotificationTypeTranslation domain) {
        if (domain == null) {
            return null;
        }

        return NotificationTypeTranslationEntity.builder()
                .translationId(domain.getTranslationId() != null ? domain.getTranslationId().intValue() : null)
                .notificationTypeId(domain.getNotificationTypeId() != null ? domain.getNotificationTypeId().intValue() : null)
                .languageId(domain.getLanguageId() != null ? domain.getLanguageId().intValue() : null)
                .translatedName(domain.getTranslatedName())
                .translatedDescription(domain.getTranslatedDescription())
                .source(toEntitySource(domain.getSource()))
                .status(toEntityStatus(domain.getStatus()))
                .build();
    }

    public NotificationTypeTranslation toDomain(NotificationTypeTranslationEntity entity) {
        if (entity == null) {
            return null;
        }

        NotificationTypeTranslation domain = new NotificationTypeTranslation();
        domain.setTranslationId(entity.getTranslationId() != null ? entity.getTranslationId().longValue() : null);
        domain.setNotificationTypeId(entity.getNotificationTypeId() != null ? entity.getNotificationTypeId().longValue() : null);
        domain.setLanguageId(entity.getLanguageId() != null ? entity.getLanguageId().longValue() : null);
        domain.setTranslatedName(entity.getTranslatedName());
        domain.setTranslatedDescription(entity.getTranslatedDescription());
        domain.setSource(toDomainSource(entity.getSource()));
        domain.setStatus(toDomainStatus(entity.getStatus()));
        return domain;
    }

    private String toEntitySource(TranslationSource source) {
        if (source == null) {
            return "MANUAL";
        }
        return source.name();
    }

    private TranslationSource toDomainSource(String source) {
        if (source == null) {
            return TranslationSource.MANUAL;
        }
        try {
            return TranslationSource.valueOf(source);
        } catch (IllegalArgumentException e) {
            return TranslationSource.MANUAL;
        }
    }

    private TranslationStatusEnum toEntityStatus(TranslationStatus status) {
        if (status == null) {
            return TranslationStatusEnum.draft;
        }
        return switch (status) {
            case DRAFT -> TranslationStatusEnum.draft;
            case PUBLISHED -> TranslationStatusEnum.published;
            case PENDING_REVIEW -> TranslationStatusEnum.pending_review;
        };
    }

    private TranslationStatus toDomainStatus(TranslationStatusEnum status) {
        if (status == null) {
            return TranslationStatus.DRAFT;
        }
        return switch (status) {
            case draft -> TranslationStatus.DRAFT;
            case published -> TranslationStatus.PUBLISHED;
            case pending_review -> TranslationStatus.PENDING_REVIEW;
        };
    }
}