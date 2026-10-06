package com.capysoft.tuevento.modules.event.application.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Respuesta de un comentario/rating de evento.
 *
 * <p>{@code rating} es nullable: null si el comentario no lleva calificación
 * (comentarios posteriores al primero, comentarios del organizador, o respuestas).
 *
 * <p>{@code isOrganizer} es true cuando el autor es el dueño del evento (R4).
 *
 * <p>{@code parentRatingId} es null para comentarios principales; contiene el
 * ratingId del padre para respuestas.
 *
 * <p>{@code editedAt} es null si el comentario no ha sido editado.
 *
 * <p>{@code editableUntil} es calculado por el servidor como
 * {@code createdAt + 2 horas} en la zona del servidor (America/Bogota).
 * El frontend debe usar este campo para decidir si mostrar el botón "Editar",
 * en vez de calcular desde {@code createdAt} en la zona del navegador.
 * Es null en los comentarios que aún no se crearon con este campo (retrocompatible).
 */
@Getter
@Builder
public class EventRatingResponse {

    private final Long          ratingId;
    private final Long          userId;
    private final String        authorName;
    private final Integer       rating;
    private final String        comment;
    private final Boolean       isVisible;
    private final Boolean       isOrganizer;
    private final LocalDateTime createdAt;
    private final LocalDateTime editedAt;
    private final Long          parentRatingId;
    /** Instante hasta el que el autor puede editar este comentario (createdAt + 2 h, zona servidor). */
    private final LocalDateTime editableUntil;
}
