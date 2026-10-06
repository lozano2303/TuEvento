package com.capysoft.tuevento.modules.language.infrastructure.persistence.mapper;

import com.capysoft.tuevento.modules.language.domain.enums.TranslationSource;
import com.capysoft.tuevento.modules.language.domain.enums.TranslationStatus;
import com.capysoft.tuevento.modules.language.domain.model.ModuleTranslation;
import com.capysoft.tuevento.modules.language.infrastructure.persistence.entity.ModuleTranslationEntity;
import com.capysoft.tuevento.modules.language.infrastructure.persistence.entity.TranslationStatusEnum;
import org.springframework.stereotype.Component;

@Component
public class ModuleTranslationMapper {

    public ModuleTranslationEntity toEntity(ModuleTranslation domain) {
        if (domain == null) {
            return null;
        }

        return ModuleTranslationEntity.builder()
                .translationId(domain.getTranslationId())
                .module(domain.getModule())
                .languageId(domain.getLanguageId())
                .translatedName(domain.getTranslatedName())
                .source(toEntitySource(domain.getSource()))
                .status(toEntityStatus(domain.getStatus()))
                .build();
    }

    public ModuleTranslation toDomain(ModuleTranslationEntity entity) {
        if (entity == null) {
            return null;
        }

        return ModuleTranslation.builder()
                .translationId(entity.getTranslationId())
                .module(entity.getModule())
                .languageId(entity.getLanguageId())
                .translatedName(entity.getTranslatedName())
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