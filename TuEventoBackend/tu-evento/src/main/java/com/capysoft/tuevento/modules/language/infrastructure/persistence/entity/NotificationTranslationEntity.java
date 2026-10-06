package com.capysoft.tuevento.modules.language.infrastructure.persistence.entity;

import com.capysoft.tuevento.shared.domain.valueobject.TranslationSource;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import com.capysoft.tuevento.shared.infrastructure.persistence.JpaAuditingEntity;
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
@EqualsAndHashCode(callSuper = false)
public class NotificationTranslationEntity extends JpaAuditingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "translation_id")
    private Long translationId;

    @Column(name = "notification_id", nullable = false)
    private Long notificationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "language_id", nullable = false)
    private LanguageEntity language;

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