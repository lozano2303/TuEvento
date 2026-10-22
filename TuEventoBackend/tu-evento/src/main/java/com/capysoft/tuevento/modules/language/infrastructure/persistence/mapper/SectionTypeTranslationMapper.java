package com.capysoft.tuevento.modules.language.infrastructure.persistence.mapper;

import com.capysoft.tuevento.shared.domain.valueobject.TranslationSource;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import com.capysoft.tuevento.modules.language.domain.model.SectionTypeTranslation;
import com.capysoft.tuevento.modules.language.infrastructure.persistence.entity.SectionTypeTranslationEntity;
import com.capysoft.tuevento.modules.language.infrastructure.persistence.entity.TranslationStatusEnum;
import org.springframework.stereotype.Component;

@Component
public class SectionTypeTranslationMapper {

    public SectionTypeTranslationEntity toEntity(SectionTypeTranslation domain) {
        if (domain == null) {
            return null;
        }

        return SectionTypeTranslationEntity.builder()
                .translationId(domain.getTranslationId())
                .sectionTypeId(domain.getSectionTypeId())
                .languageId(domain.getLanguageId())
                .translatedName(domain.getTranslatedName())
                .source(toEntitySource(domain.getSource()))
                .status(toEntityStatus(domain.getStatus()))
                .build();
    }

    public SectionTypeTranslation toDomain(SectionTypeTranslationEntity entity) {
        if (entity == null) {
            return null;
        }

        SectionTypeTranslation domain = new SectionTypeTranslation();
        domain.setTranslationId(entity.getTranslationId());
        domain.setSectionTypeId(entity.getSectionTypeId());
        domain.setLanguageId(entity.getLanguageId());
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