package com.capysoft.tuevento.modules.language.infrastructure.persistence.entity;

import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Entidad JPA para event_rating_translation.
 */
@Entity
@Table(name = "event_rating_translation")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class EventRatingTranslationEntity  {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "translation_id")
    private Integer translationId;

    @Column(name = "rating_id", nullable = false)
    private Integer ratingId;

    @Column(name = "language_id", nullable = false)
    private Integer languageId;

    @Column(name = "translated_comment", length = 255)
    private String translatedComment;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private TranslationStatus status = TranslationStatus.DRAFT;
}