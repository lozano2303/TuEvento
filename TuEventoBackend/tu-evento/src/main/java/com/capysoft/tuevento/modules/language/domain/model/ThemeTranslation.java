package com.capysoft.tuevento.modules.language.domain.model;

import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationSource;

import java.util.Objects;

public class ThemeTranslation {
    private Long translationId;
    private Integer themeId;  // Changed from Long to Integer to match database
    private Long languageId;
    private String translatedName;
    private String translatedDescription;
    private TranslationSource source;
    private TranslationStatus status;

    public ThemeTranslation() {}

    public ThemeTranslation(Integer themeId, Long languageId, String translatedName, 
                           String translatedDescription, TranslationSource source, TranslationStatus status) {
        this.themeId = themeId;
        this.languageId = languageId;
        this.translatedName = translatedName;
        this.translatedDescription = translatedDescription;
        this.source = source;
        this.status = status;
    }

    // Business validations
    public void validateTranslatedContent() {
        if (translatedName == null || translatedName.trim().isEmpty()) {
            throw new IllegalArgumentException("Translated name cannot be null or empty");
        }
        if (translatedDescription == null || translatedDescription.trim().isEmpty()) {
            throw new IllegalArgumentException("Translated description cannot be null or empty");
        }
    }

    public void normalizeContent() {
        if (translatedName != null) {
            this.translatedName = translatedName.trim();
        }
        if (translatedDescription != null) {
            this.translatedDescription = translatedDescription.trim();
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

    public Integer getThemeId() {
        return themeId;
    }

    public void setThemeId(Integer themeId) {
        this.themeId = themeId;
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

    public String getTranslatedDescription() {
        return translatedDescription;
    }

    public void setTranslatedDescription(String translatedDescription) {
        this.translatedDescription = translatedDescription;
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
        ThemeTranslation that = (ThemeTranslation) o;
        return Objects.equals(themeId, that.themeId) &&
               Objects.equals(languageId, that.languageId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(themeId, languageId);
    }
}