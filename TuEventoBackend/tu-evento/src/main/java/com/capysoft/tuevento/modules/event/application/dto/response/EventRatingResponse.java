package com.capysoft.tuevento.modules.event.application.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Respuesta de un comentario/rating de evento.
 *
 * <p>{@code rating} es nullable: null si el comentario no lleva calificación
 * (comentarios posteriores al primero, o comentarios del organizador del evento).
 *
 * <p>{@code isOrganizer} es true cuando el autor es el dueño del evento (R4).
 */
@Getter
@Builder
public class EventRatingResponse {

    private final Long ratingId;
    private final Long userId;
    private final String authorName;
    private final Integer rating;
    private final String comment;
    private final Boolean isVisible;
    private final Boolean isOrganizer;
    private final LocalDateTime createdAt;
}
