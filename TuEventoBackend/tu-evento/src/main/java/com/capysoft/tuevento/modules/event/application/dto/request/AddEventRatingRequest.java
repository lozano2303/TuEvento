package com.capysoft.tuevento.modules.event.application.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Cuerpo del POST /api/v1/events/{eventId}/ratings.
 *
 * <p>El campo {@code rating} es opcional a nivel de request:
 * el service aplica las reglas de negocio R3/R4 para determinar si
 * el valor se usa, se ignora o debe ser null. El cliente puede enviarlo
 * siempre; el service lo sobreescribe si corresponde.
 *
 * <p>El campo {@code comment} se hace trim() en el service antes de validar.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddEventRatingRequest {

    /** 1–5 cuando aplica (R3). Ignorado si es el organizador (R4) o ya tiene uno (R3). */
    @Min(1)
    @Max(5)
    private Integer rating;

    @NotBlank
    @Size(max = 500)
    private String comment;
}
