package com.capysoft.tuevento.modules.language.domain.model;

import com.capysoft.tuevento.shared.domain.valueobject.TranslationSource;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import jakarta.persistence.*;
import lombok.*;

/**
 * Entidad para traducción de notificaciones.
 */
@Entity
@Table(name = "notification_translation")
@Getter
@Setter
@NoArgsConstructor
@Builder
public class NotificationTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "translation_id")
    private Integer translationId;

    @Column(name = "notification_id", nullable = false)
    private Integer notificationId;

    @Column(name = "language_id", nullable = false)
    private Integer languageId;

    @Column(name = "translated_subject", length = 100)
    private String translatedSubject;

    @Column(name = "translated_body", length = 255)
    private String translatedBody;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 50)
    private TranslationSource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TranslationStatus status;

    @java.beans.ConstructorProperties({"translationId", "notificationId", "languageId", "translatedSubject", "translatedBody", "source", "status"})
    @java.lang.SuppressWarnings("all")
    public NotificationTranslation(final Integer translationId, final Integer notificationId, final Integer languageId, final String translatedSubject, final String translatedBody, final TranslationSource source, final TranslationStatus status) {
        this.translationId = translationId;
        this.notificationId = notificationId;
        this.languageId = languageId;
        this.translatedSubject = translatedSubject;
        this.translatedBody = translatedBody;
        this.source = source;
        this.status = status;
    }
}
