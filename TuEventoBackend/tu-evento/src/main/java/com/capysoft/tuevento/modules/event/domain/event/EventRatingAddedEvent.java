package com.capysoft.tuevento.modules.event.domain.event;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRatingAddedEvent {

    private Long ratingId;
    private Long eventId;
    private Long userId;
    /** Nullable: null si el comentario no lleva calificación (R3/R4/respuestas). */
    private Integer rating;
    private String comment;
    private Boolean isVisible;
    /** True si el autor es el organizador del evento (R4). */
    private Boolean isOrganizer;
    /**
     * Nullable. Si tiene valor, este es una respuesta al comentario con ese ratingId.
     */
    private Long parentRatingId;
    /** ID del usuario al que se responde directamente (para menciones). */
    private Long replyToUserId;
    /** Nombre del usuario al que se responde. */
    private String replyToUserName;
    private LocalDateTime occurredAt;
}
