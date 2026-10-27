package com.capysoft.tuevento.modules.profile.infrastructure.persistence.entity;

import com.capysoft.tuevento.shared.infrastructure.persistence.TranslationEntityBase;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Entidad JPA para profile_translation.
 */
@Entity
@Table(name = "profile_translation")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class ProfileTranslationEntity extends TranslationEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "translation_id")
    private Integer translationId;

    @Column(name = "profile_id", nullable = false)
    private Integer profileId;

    @Column(name = "language_id", nullable = false)
    private Integer languageId;

    @Column(name = "translated_bio", nullable = false, length = 255)
    private String translatedBio;
}
