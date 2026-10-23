package com.capysoft.tuevento.modules.seat.application.dto.request;

import com.capysoft.tuevento.modules.seat.domain.model.SeatType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSeatRequest {

    @NotNull(message = "El ID del bloque es obligatorio")
    private Integer seatBlockId;

    @NotNull(message = "El ID de la sección es obligatorio")
    private Integer eventSectionId;

    @NotBlank(message = "El código es obligatorio")
    @Size(max = 20, message = "El código no puede superar 20 caracteres")
    private String code;

    @NotNull(message = "La fila es obligatoria")
    @Positive(message = "La fila debe ser mayor a 0")
    private Integer row;

    @NotNull(message = "La posición es obligatoria")
    @Positive(message = "La posición debe ser mayor a 0")
    private Integer position;

    @NotNull(message = "El tipo es obligatorio")
    private SeatType type;
}
