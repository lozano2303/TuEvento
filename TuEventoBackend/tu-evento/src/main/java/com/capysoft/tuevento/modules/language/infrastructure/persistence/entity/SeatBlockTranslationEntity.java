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
 * Entidad JPA para seat_block_translation.
 */
@Entity
@Table(name = "seat_block_translation")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class SeatBlockTranslationEntity extends JpaAuditingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "translation_id")
    private Long translationId;

    @Column(name = "seat_block_id", nullable = false)
    private Long seatBlockId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "language_id", nullable = false)
    private LanguageEntity language;

    @Column(name = "translated_name", nullable = false, length = 100)
    private String translatedName;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 50)
    private TranslationSource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private TranslationStatus status = TranslationStatus.DRAFT;
}