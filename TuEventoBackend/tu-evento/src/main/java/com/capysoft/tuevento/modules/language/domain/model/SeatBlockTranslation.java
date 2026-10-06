package com.capysoft.tuevento.modules.language.domain.model;

import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationSource;

import java.util.Objects;

public class SeatBlockTranslation {
    private Long translationId;
    private Long seatBlockId;
    private Long languageId;
    private String translatedName;
    private TranslationSource source;
    private TranslationStatus status;

    public SeatBlockTranslation() {}

    public SeatBlockTranslation(Long seatBlockId, Long languageId, String translatedName, 
                               TranslationSource source, TranslationStatus status) {
        this.seatBlockId = seatBlockId;
        this.languageId = languageId;
        this.translatedName = translatedName;
        this.source = source;
        this.status = status;
    }

    // Business validations
    public void validateTranslatedContent() {
        if (translatedName == null || translatedName.trim().isEmpty()) {
            throw new IllegalArgumentException("Translated name cannot be null or empty");
        }
    }

    public void normalizeContent() {
        if (translatedName != null) {
            this.translatedName = translatedName.trim();
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

    public Long getSeatBlockId() {
        return seatBlockId;
    }

    public void setSeatBlockId(Long seatBlockId) {
        this.seatBlockId = seatBlockId;
    }

    public Long getLanguageId() {
        return languageId;
    }

    public void setLanguageId(Long languageId) {
        this.languageId = languageId;
    }

    public String getTranslatedName() {
        return translatedName;
    }

    public void setTranslatedName(String translatedName) {
        this.translatedName = translatedName;
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
        SeatBlockTranslation that = (SeatBlockTranslation) o;
        return Objects.equals(seatBlockId, that.seatBlockId) &&
               Objects.equals(languageId, that.languageId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(seatBlockId, languageId);
    }
}