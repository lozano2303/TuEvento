package com.capysoft.tuevento.modules.theme.infrastructure.persistence.entity;

import com.capysoft.tuevento.shared.infrastructure.persistence.TranslationEntityBase;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Entidad JPA para theme_translation.
 */
@Entity
@Table(name = "theme_translation")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class ThemeTranslationEntity extends TranslationEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "translation_id")
    private Integer translationId;

    @Column(name = "theme_id", nullable = false)
    private Integer themeId;

    @Column(name = "language_id", nullable = false)
    private Integer languageId;

    @Column(name = "translated_name", nullable = false, length = 100)
    private String translatedName;

    @Column(name = "translated_description", nullable = false, length = 255)
    private String translatedDescription;
}
