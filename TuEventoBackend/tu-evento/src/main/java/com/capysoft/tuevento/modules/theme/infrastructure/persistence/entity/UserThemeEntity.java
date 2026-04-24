package com.capysoft.tuevento.modules.theme.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_theme")
public class UserThemeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_theme_id")
    private Integer userThemeId;

    /** FK to user — no @ManyToOne to avoid cross-module coupling. */
    @Column(name = "user_id", nullable = false)
    private Integer userId;

    /** FK to theme — no @ManyToOne to avoid cross-module coupling. */
    @Column(name = "theme_id", nullable = false)
    private Integer themeId;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
