package com.capysoft.tuevento.modules.section.application.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateEventSectionRequest {

    @NotNull(message = "El ID del evento es obligatorio")
    private Integer eventId;

    @NotNull(message = "El tipo de sección es obligatorio")
    private Integer sectionTypeId;

    @NotNull(message = "La capacidad es obligatoria")
    @Min(value = 1, message = "La capacidad debe ser mayor a 0")
    private Integer capacity;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.0", inclusive = true, message = "El precio no puede ser negativo")
    private BigDecimal price;
}
