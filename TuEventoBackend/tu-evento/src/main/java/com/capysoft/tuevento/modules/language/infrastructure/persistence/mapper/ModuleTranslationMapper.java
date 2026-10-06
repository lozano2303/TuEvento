package com.capysoft.tuevento.modules.language.infrastructure.persistence.mapper;

import com.capysoft.tuevento.shared.domain.valueobject.TranslationSource;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
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
                .translationId(domain.getTranslationId() != null ? domain.getTranslationId().intValue() : null)
                .module(domain.getModule())
                .languageId(domain.getLanguageId() != null ? domain.getLanguageId().intValue() : null)
                .translatedName(domain.getTranslatedName())
                .source(toEntitySource(domain.getSource()))
                .status(toEntityStatus(domain.getStatus()))
                .build();
    }

    public ModuleTranslation toDomain(ModuleTranslationEntity entity) {
        if (entity == null) {
            return null;
        }

        ModuleTranslation domain = new ModuleTranslation();
        domain.setTranslationId(entity.getTranslationId() != null ? entity.getTranslationId().longValue() : null);
        domain.setModule(entity.getModule());
        domain.setLanguageId(entity.getLanguageId() != null ? entity.getLanguageId().longValue() : null);
        domain.setTranslatedName(entity.getTranslatedName());
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