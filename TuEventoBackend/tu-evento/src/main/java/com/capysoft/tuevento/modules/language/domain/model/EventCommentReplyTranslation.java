package com.capysoft.tuevento.modules.language.domain.model;

import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;

import java.util.Objects;

public class EventCommentReplyTranslation {
    private Long translationId;
    private Long replyId;
    private Long languageId;
    private String translatedReplyText;
    private TranslationStatus status;

    public EventCommentReplyTranslation() {}

    public EventCommentReplyTranslation(Long replyId, Long languageId, String translatedReplyText, 
                                      TranslationStatus status) {
        this.replyId = replyId;
        this.languageId = languageId;
        this.translatedReplyText = translatedReplyText;
        this.status = status;
    }

    // Business validations
    public void validateTranslatedContent() {
        if (translatedReplyText != null && translatedReplyText.trim().isEmpty()) {
            throw new IllegalArgumentException("Translated reply text cannot be empty if provided");
        }
    }

    public void normalizeContent() {
        if (translatedReplyText != null) {
            this.translatedReplyText = translatedReplyText.trim();
        }
    }

    public void markAsPublished() {
        if (status != TranslationStatus.DRAFT && status != TranslationStatus.PENDING_REVIEW) {
            throw new IllegalStateException("Only DRAFT or PENDING_REVIEW translations can be published");
        }
        this.status = TranslationStatus.PUBLISHED;
    }

    public void markAsPendingReview() {
        if (status != TranslationStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT translations can be marked as pending review");
        }
        this.status = TranslationStatus.PENDING_REVIEW;
    }

    // Getters and Setters
    public Long getTranslationId() {
        return translationId;
    }

    public void setTranslationId(Long translationId) {
        this.translationId = translationId;
    }

    public Long getReplyId() {
        return replyId;
    }

    public void setReplyId(Long replyId) {
        this.replyId = replyId;
    }

    public Long getLanguageId() {
        return languageId;
    }

    public void setLanguageId(Long languageId) {
        this.languageId = languageId;
    }

    public String getTranslatedReplyText() {
        return translatedReplyText;
    }

    public void setTranslatedReplyText(String translatedReplyText) {
        this.translatedReplyText = translatedReplyText;
    }

    public TranslationStatus getStatus() {
        return status;
    }

    public void setStatus(TranslationStatus status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventCommentReplyTranslation that = (EventCommentReplyTranslation) o;
        return Objects.equals(replyId, that.replyId) &&
               Objects.equals(languageId, that.languageId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(replyId, languageId);
    }
}