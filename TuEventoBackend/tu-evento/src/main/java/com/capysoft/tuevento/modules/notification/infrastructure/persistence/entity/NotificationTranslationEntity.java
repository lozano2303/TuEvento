package com.capysoft.tuevento.modules.notification.infrastructure.persistence.entity;

import com.capysoft.tuevento.shared.infrastructure.persistence.TranslationEntityBase;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Entidad JPA para notification_translation.
 */
@Entity
@Table(name = "notification_translation")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class NotificationTranslationEntity extends TranslationEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "translation_id")
    private Integer translationId;

    @Column(name = "notification_id", nullable = false)
    private Integer notificationId;

    @Column(name = "language_id", nullable = false)
    private Integer languageId;

    @Column(name = "translated_subject", columnDefinition = "text")
    private String translatedSubject;

    @Column(name = "translated_body", columnDefinition = "text")
    private String translatedBody;
}
