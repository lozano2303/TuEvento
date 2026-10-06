package com.capysoft.tuevento.modules.event.domain.model;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRating {

    private Long ratingId;
    private Long eventId;
    private Long userId;
    /** Nullable: null si R3/R4 no aplican calificación, o si es respuesta. */
    private Integer rating;
    private String comment;
    private Boolean isVisible;
    private LocalDateTime createdAt;
    /** Nullable: null cuando es editado_at aún no se tiene (se añade en commit 2). */
    private LocalDateTime editedAt;
    /**
     * Nullable. Si es null, este registro es un comentario principal.
     * Si tiene valor, es una respuesta al comentario con ese ratingId.
     */
    private Long parentRatingId;
}
