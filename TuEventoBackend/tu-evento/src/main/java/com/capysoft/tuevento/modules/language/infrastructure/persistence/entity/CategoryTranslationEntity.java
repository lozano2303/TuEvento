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
 * Entidad JPA para category_translation.
 */
@Entity
@Table(name = "category_translation")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class CategoryTranslationEntity  {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "translation_id")
    private Integer translationId;

    @Column(name = "category_id", nullable = false)
    private Integer categoryId;

    @Column(name = "language_id", nullable = false)
    private Integer languageId;

    @Column(name = "translated_name", nullable = false, length = 100)
    private String translatedName;

    @Column(name = "translated_description", nullable = false, length = 255)
    private String translatedDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 50)
    private TranslationSource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private TranslationStatus status = TranslationStatus.DRAFT;
}