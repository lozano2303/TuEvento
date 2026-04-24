package com.capysoft.tuevento.modules.theme.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Append-only audit log entity — never updated after insertion.
 * Does not extend JpaAuditingEntity intentionally.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "theme_log")
public class ThemeLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Integer logId;

    /** FK to user_theme — no @ManyToOne to avoid cross-module coupling. */
    @Column(name = "user_theme_id", nullable = false)
    private Integer userThemeId;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "property", length = 100)
    private String property;

    @Column(name = "old_value", length = 255)
    private String oldValue;

    @Column(name = "new_value", length = 255)
    private String newValue;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
