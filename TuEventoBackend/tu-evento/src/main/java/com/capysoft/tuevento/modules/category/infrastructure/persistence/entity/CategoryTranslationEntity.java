package com.capysoft.tuevento.modules.category.infrastructure.persistence.entity;

import com.capysoft.tuevento.shared.infrastructure.persistence.TranslationEntityBase;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Entidad JPA para category_translation. {@code source} y {@code status} vienen de
 * {@link TranslationEntityBase}.
 */
@Entity
@Table(name = "category_translation")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class CategoryTranslationEntity extends TranslationEntityBase {

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
}
