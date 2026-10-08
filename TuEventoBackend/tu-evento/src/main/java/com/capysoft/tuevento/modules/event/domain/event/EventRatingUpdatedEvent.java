package com.capysoft.tuevento.modules.event.domain.event;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Evento de dominio publicado tras editar un comentario (AFTER_COMMIT).
 * El listener lo envía por WebSocket al canal /topic/events/{eventId}/comments/updated.
 *
 * <p>Contiene todos los campos del payload del GET /ratings para que el frontend
 * pueda reemplazar el comentario completo por ratingId sin perder campos.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRatingUpdatedEvent {

    private Long          ratingId;
    private Long          eventId;
    private Long          userId;
    private String        authorName;
    private Integer       rating;
    private String        comment;
    private Boolean       isVisible;
    private Boolean       isOrganizer;
    private LocalDateTime createdAt;
    private LocalDateTime editedAt;
    /** Null para comentarios principales; ratingId del padre para respuestas. */
    private Long          parentRatingId;
    /** ID del usuario al que se responde directamente (para menciones). */
    private Long          replyToUserId;
    /** Nombre del usuario al que se responde. */
    private String        replyToUserName;
}
