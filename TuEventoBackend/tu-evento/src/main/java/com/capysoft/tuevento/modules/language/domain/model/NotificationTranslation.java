package com.capysoft.tuevento.modules.language.domain.model;

import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationSource;

import java.util.Objects;

public class NotificationTranslation {
    private Integer translationId;
    private Integer notificationId;
    private Integer languageId;
    private String translatedSubject;
    private String translatedBody;
    private TranslationSource source;
    private TranslationStatus status;

    public NotificationTranslation() {}

    public NotificationTranslation(Integer notificationId, Integer languageId, String translatedSubject, 
                                 String translatedBody, TranslationSource source, TranslationStatus status) {
        this.notificationId = notificationId;
        this.languageId = languageId;
        this.translatedSubject = translatedSubject;
        this.translatedBody = translatedBody;
        this.source = source;
        this.status = status;
    }

    // Business validations
    public void validateTranslatedContent() {
        if (translatedSubject != null && translatedSubject.trim().isEmpty()) {
            throw new IllegalArgumentException("Translated subject cannot be empty if provided");
        }
        if (translatedBody != null && translatedBody.trim().isEmpty()) {
            throw new IllegalArgumentException("Translated body cannot be empty if provided");
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

    public Integer getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(Integer notificationId) {
        this.notificationId = notificationId;
    }

    public Integer getLanguageId() {
        return languageId;
    }

    public void setLanguageId(Integer languageId) {
        this.languageId = languageId;
    }

    public String getTranslatedSubject() {
        return translatedSubject;
    }

    public void setTranslatedSubject(String translatedSubject) {
        this.translatedSubject = translatedSubject;
    }

    public String getTranslatedBody() {
        return translatedBody;
    }

    public void setTranslatedBody(String translatedBody) {
        this.translatedBody = translatedBody;
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
        NotificationTranslation that = (NotificationTranslation) o;
        return Objects.equals(notificationId, that.notificationId) &&
               Objects.equals(languageId, that.languageId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(notificationId, languageId);
    }
}