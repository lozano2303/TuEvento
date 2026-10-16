package com.capysoft.tuevento.modules.notification.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO para traducción de notificación.
 */
@Getter
@Builder
@NoArgsConstructor
public class NotificationTranslationDTO {
    private String languageCode;
    private String translatedSubject;
    private String translatedBody;
    private String status;
    private String source;

    @java.beans.ConstructorProperties({"languageCode", "translatedSubject", "translatedBody", "status", "source"})
    @java.lang.SuppressWarnings("all")
    public NotificationTranslationDTO(final String languageCode, final String translatedSubject, final String translatedBody, final String status, final String source) {
        this.languageCode = languageCode;
        this.translatedSubject = translatedSubject;
        this.translatedBody = translatedBody;
        this.status = status;
        this.source = source;
    }
}
