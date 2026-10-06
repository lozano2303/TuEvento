package com.capysoft.tuevento.modules.language.infrastructure.persistence.mapper;

import com.capysoft.tuevento.modules.language.domain.enums.TranslationSource;
import com.capysoft.tuevento.modules.language.domain.enums.TranslationStatus;
import com.capysoft.tuevento.modules.language.domain.model.ActionTranslation;
import com.capysoft.tuevento.modules.language.infrastructure.persistence.entity.ActionTranslationEntity;
import com.capysoft.tuevento.modules.language.infrastructure.persistence.entity.TranslationStatusEnum;
import org.springframework.stereotype.Component;

@Component
public class ActionTranslationMapper {

    public ActionTranslationEntity toEntity(ActionTranslation domain) {
        if (domain == null) {
            return null;
        }

        return ActionTranslationEntity.builder()
                .translationId(domain.getTranslationId())
                .action(domain.getAction())
                .languageId(domain.getLanguageId())
                .translatedDescription(domain.getTranslatedDescription())
                .source(toEntitySource(domain.getSource()))
                .status(toEntityStatus(domain.getStatus()))
                .build();
    }

    public ActionTranslation toDomain(ActionTranslationEntity entity) {
        if (entity == null) {
            return null;
        }

        return ActionTranslation.builder()
                .translationId(entity.getTranslationId())
                .action(entity.getAction())
                .languageId(entity.getLanguageId())
                .translatedDescription(entity.getTranslatedDescription())
                .source(toDomainSource(entity.getSource()))
                .status(toDomainStatus(entity.getStatus()))
                .build();
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