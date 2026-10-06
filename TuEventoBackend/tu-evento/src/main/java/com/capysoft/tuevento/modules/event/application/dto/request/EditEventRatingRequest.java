package com.capysoft.tuevento.modules.event.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Cuerpo del PATCH /api/v1/events/{eventId}/ratings/{ratingId}.
 * Solo permite cambiar el texto del comentario (B).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EditEventRatingRequest {

    @NotBlank
    @Size(max = 500)
    private String comment;
}
