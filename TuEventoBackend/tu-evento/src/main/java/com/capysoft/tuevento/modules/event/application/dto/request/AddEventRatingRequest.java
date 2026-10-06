package com.capysoft.tuevento.modules.event.application.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Cuerpo del POST /api/v1/events/{eventId}/ratings.
 *
 * <p>{@code rating} es opcional:
 * el service aplica R3/R4 para determinar si se usa. Si se envía
 * {@code parentRatingId}, el service ignora el rating (respuestas → null).
 *
 * <p>{@code parentRatingId} es opcional. Si se envía, el comentario
 * es una respuesta al comentario con ese id. El service valida que
 * el padre exista, sea del mismo evento y sea principal (parent_id nulo).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddEventRatingRequest {

    /** 1–5 cuando aplica (R3). Ignorado si es respuesta, organizador (R4) o ya tiene uno (R3). */
    @Min(1)
    @Max(5)
    private Integer rating;

    @NotBlank
    @Size(max = 500)
    private String comment;

    /** Nullable. Si se envía, este comentario es una respuesta al comentario con este id. */
    private Long parentRatingId;
}
