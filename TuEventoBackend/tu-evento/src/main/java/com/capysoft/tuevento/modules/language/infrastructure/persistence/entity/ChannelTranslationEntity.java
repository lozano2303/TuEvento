package com.capysoft.tuevento.modules.language.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "channel_translation")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelTranslationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "translation_id")
    private Integer translationId;

    @Column(name = "channel_id", nullable = false)
    private Integer channelId;

    @Column(name = "language_id", nullable = false)
    private Integer languageId;

    @Column(name = "translated_name", length = 100)
    private String translatedName;

    @Column(name = "translated_description", length = 255, nullable = false)
    private String translatedDescription;

    @Column(name = "source", length = 50, nullable = false)
    private String source;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TranslationStatusEnum status = TranslationStatusEnum.draft;
}