package com.capysoft.tuevento.modules.language.domain.model;

import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;

import java.util.Objects;

public class EventRatingTranslation {
    private Long translationId;
    private Long ratingId;
    private Long languageId;
    private String translatedComment;
    private TranslationStatus status;

    public EventRatingTranslation() {}

    public EventRatingTranslation(Long ratingId, Long languageId, String translatedComment, 
                                TranslationStatus status) {
        this.ratingId = ratingId;
        this.languageId = languageId;
        this.translatedComment = translatedComment;
        this.status = status;
    }

    // Business validations
    public void validateTranslatedContent() {
        if (translatedComment != null && translatedComment.trim().isEmpty()) {
            throw new IllegalArgumentException("Translated comment cannot be empty if provided");
        }
    }

    public void normalizeContent() {
        if (translatedComment != null) {
            this.translatedComment = translatedComment.trim();
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

    public Long getRatingId() {
        return ratingId;
    }

    public void setRatingId(Long ratingId) {
        this.ratingId = ratingId;
    }

    public Long getLanguageId() {
        return languageId;
    }

    public void setLanguageId(Long languageId) {
        this.languageId = languageId;
    }

    public String getTranslatedComment() {
        return translatedComment;
    }

    public void setTranslatedComment(String translatedComment) {
        this.translatedComment = translatedComment;
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
        EventRatingTranslation that = (EventRatingTranslation) o;
        return Objects.equals(ratingId, that.ratingId) &&
               Objects.equals(languageId, that.languageId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ratingId, languageId);
    }
}