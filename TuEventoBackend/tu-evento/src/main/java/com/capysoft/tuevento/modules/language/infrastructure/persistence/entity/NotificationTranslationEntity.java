package com.capysoft.tuevento.modules.language.infrastructure.persistence.entity;

import com.capysoft.tuevento.shared.domain.valueobject.TranslationSource;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Entidad JPA para notification_translation.
 */
@Entity
@Table(name = "notification_translation")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class NotificationTranslationEntity  {

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
    @Builder.Default
    private TranslationStatus status = TranslationStatus.DRAFT;
}