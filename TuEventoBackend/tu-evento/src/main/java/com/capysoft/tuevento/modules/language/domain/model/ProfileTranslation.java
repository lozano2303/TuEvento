package com.capysoft.tuevento.modules.language.domain.model;

import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationSource;

import java.util.Objects;

public class ProfileTranslation {
    private Long translationId;
    private Long profileId;
    private Long languageId;
    private String translatedBio;
    private TranslationSource source;
    private TranslationStatus status;

    public ProfileTranslation() {}

    public ProfileTranslation(Long profileId, Long languageId, String translatedBio, 
                             TranslationSource source, TranslationStatus status) {
        this.profileId = profileId;
        this.languageId = languageId;
        this.translatedBio = translatedBio;
        this.source = source;
        this.status = status;
    }

    // Business validations
    public void validateTranslatedContent() {
        if (translatedBio == null || translatedBio.trim().isEmpty()) {
            throw new IllegalArgumentException("Translated bio cannot be null or empty");
        }
    }

    public void normalizeContent() {
        if (translatedBio != null) {
            this.translatedBio = translatedBio.trim();
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

    public Long getProfileId() {
        return profileId;
    }

    public void setProfileId(Long profileId) {
        this.profileId = profileId;
    }

    public Long getLanguageId() {
        return languageId;
    }

    public void setLanguageId(Long languageId) {
        this.languageId = languageId;
    }

    public String getTranslatedBio() {
        return translatedBio;
    }

    public void setTranslatedBio(String translatedBio) {
        this.translatedBio = translatedBio;
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
        ProfileTranslation that = (ProfileTranslation) o;
        return Objects.equals(profileId, that.profileId) &&
               Objects.equals(languageId, that.languageId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(profileId, languageId);
    }
}