package com.capysoft.tuevento.modules.event.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "event_rating")
public class EventRatingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rating_id")
    private Long ratingId;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "rating")
    private Integer rating;

    @Column(name = "comment", nullable = false)
    private String comment;

    @Column(name = "is_visible", nullable = false)
    private Boolean isVisible;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** Nullable. Se rellena al editar el comentario (commit 2). */
    @Column(name = "edited_at")
    private LocalDateTime editedAt;

    /**
     * Nullable. Si es null, es un comentario principal.
     * Si tiene valor, es una respuesta al comentario con ese ratingId.
     * FK auto-referencial con ON DELETE CASCADE (migración 088).
     */
    @Column(name = "parent_rating_id")
    private Long parentRatingId;
}
