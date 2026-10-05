package com.capysoft.tuevento.modules.language.infrastructure.persistence.entity;

import com.capysoft.tuevento.shared.domain.valueobject.TranslationJobStatus;
import com.capysoft.tuevento.shared.infrastructure.persistence.JpaAuditingEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entidad JPA para translation_job.
 */
@Entity
@Table(name = "translation_job")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class TranslationJobEntity extends JpaAuditingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "job_id")
    private Long jobId;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_language_id", nullable = false)
    private LanguageEntity sourceLanguage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_language_id", nullable = false)
    private LanguageEntity targetLanguage;

    @Column(name = "source_hash", nullable = false, length = 64)
    private String sourceHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TranslationJobStatus status;

    @Column(name = "provider", nullable = false, length = 20)
    private String provider;

    @Column(name = "attempts", nullable = false)
    @Builder.Default
    private Integer attempts = 0;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}