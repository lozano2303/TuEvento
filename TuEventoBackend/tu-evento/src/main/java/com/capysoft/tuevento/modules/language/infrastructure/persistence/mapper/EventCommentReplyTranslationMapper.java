package com.capysoft.tuevento.modules.language.infrastructure.persistence.mapper;

import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import com.capysoft.tuevento.modules.language.domain.model.EventCommentReplyTranslation;
import com.capysoft.tuevento.modules.language.infrastructure.persistence.entity.EventCommentReplyTranslationEntity;
import com.capysoft.tuevento.modules.language.infrastructure.persistence.entity.TranslationStatusEnum;
import org.springframework.stereotype.Component;

@Component
public class EventCommentReplyTranslationMapper {

    public EventCommentReplyTranslationEntity toEntity(EventCommentReplyTranslation domain) {
        if (domain == null) {
            return null;
        }

        return EventCommentReplyTranslationEntity.builder()
                .translationId(domain.getTranslationId())
                .replyId(domain.getReplyId())
                .languageId(domain.getLanguageId())
                .translatedReplyText(domain.getTranslatedReplyText())
                .status(toEntityStatus(domain.getStatus()))
                .build();
    }

    public EventCommentReplyTranslation toDomain(EventCommentReplyTranslationEntity entity) {
        if (entity == null) {
            return null;
        }

        EventCommentReplyTranslation domain = new EventCommentReplyTranslation();
        domain.setTranslationId(entity.getTranslationId());
        domain.setReplyId(entity.getReplyId());
        domain.setLanguageId(entity.getLanguageId());
        domain.setTranslatedReplyText(entity.getTranslatedReplyText());
        domain.setStatus(toDomainStatus(entity.getStatus()));
        return domain;
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