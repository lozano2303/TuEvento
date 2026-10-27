package com.capysoft.tuevento.modules.event.infrastructure.persistence.entity;

import com.capysoft.tuevento.shared.infrastructure.persistence.TranslationEntityBase;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Entidad JPA para event_translation. {@code source} y {@code status} vienen de
 * {@link TranslationEntityBase}.
 */
@Entity
@Table(name = "event_translation")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class EventTranslationEntity extends TranslationEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "translation_id")
    private Integer translationId;

    @Column(name = "event_id", nullable = false)
    private Integer eventId;

    @Column(name = "language_id", nullable = false)
    private Integer languageId;

    @Column(name = "translated_name", length = 100)
    private String translatedName;

    @Column(name = "translated_description", length = 255)
    private String translatedDescription;
}
