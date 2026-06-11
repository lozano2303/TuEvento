package com.capysoft.tuevento.modules.section.application.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEventSectionRequest {

    @Min(value = 1, message = "La capacidad debe ser mayor a 0")
    private Integer capacity;

    @DecimalMin(value = "0.0", inclusive = true, message = "El precio no puede ser negativo")
    private BigDecimal price;

    private Boolean isActive;
}
