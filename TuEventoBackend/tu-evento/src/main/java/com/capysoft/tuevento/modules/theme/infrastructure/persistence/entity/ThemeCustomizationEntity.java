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
@Table(name = "theme_customization")
public class ThemeCustomizationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customization_id")
    private Integer customizationId;

    /** FK to user_theme — no @ManyToOne to avoid cross-module coupling. */
    @Column(name = "user_theme_id", nullable = false)
    private Integer userThemeId;

    @Column(name = "property", nullable = false, length = 100)
    private String property;

    @Column(name = "value", nullable = false, length = 255)
    private String value;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
