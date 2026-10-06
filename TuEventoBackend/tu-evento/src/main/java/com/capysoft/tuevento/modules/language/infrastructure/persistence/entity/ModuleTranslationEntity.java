package com.capysoft.tuevento.modules.language.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "module_translation")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModuleTranslationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "translation_id")
    private Integer translationId;

    @Column(name = "module", length = 50, nullable = false)
    private String module;

    @Column(name = "language_id", nullable = false)
    private Integer languageId;

    @Column(name = "translated_name", length = 100, nullable = false)
    private String translatedName;

    @Column(name = "source", length = 50, nullable = false)
    private String source;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TranslationStatusEnum status = TranslationStatusEnum.draft;
}