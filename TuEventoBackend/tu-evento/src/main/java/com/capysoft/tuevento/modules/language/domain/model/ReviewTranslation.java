package com.capysoft.tuevento.modules.language.domain.model;

import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationSource;

import java.util.Objects;

public class ReviewTranslation {
    private Integer translationId;
    private Integer reviewId;
    private Integer languageId;
    private String translatedComment;
    private TranslationSource source;
    private TranslationStatus status;

    public ReviewTranslation() {}

    public ReviewTranslation(Integer reviewId, Integer languageId, String translatedComment, 
                            TranslationSource source, TranslationStatus status) {
        this.reviewId = reviewId;
        this.languageId = languageId;
        this.translatedComment = translatedComment;
        this.source = source;
        this.status = status;
    }

    // Business validations
    public void validateTranslatedContent() {
        if (translatedComment == null || translatedComment.trim().isEmpty()) {
            throw new IllegalArgumentException("Translated comment cannot be null or empty");
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
    public Integer getTranslationId() {
        return translationId;
    }

    public void setTranslationId(Integer translationId) {
        this.translationId = translationId;
    }

    public Integer getReviewId() {
        return reviewId;
    }

    public void setReviewId(Integer reviewId) {
        this.reviewId = reviewId;
    }

    public Integer getLanguageId() {
        return languageId;
    }

    public void setLanguageId(Integer languageId) {
        this.languageId = languageId;
    }

    public String getTranslatedComment() {
        return translatedComment;
    }

    public void setTranslatedComment(String translatedComment) {
        this.translatedComment = translatedComment;
    }

    public TranslationSource getSource() {
        return source;
    }

    public void setSource(TranslationSource source) {
        this.source = source;
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
        ReviewTranslation that = (ReviewTranslation) o;
        return Objects.equals(reviewId, that.reviewId) &&
               Objects.equals(languageId, that.languageId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reviewId, languageId);
    }
}