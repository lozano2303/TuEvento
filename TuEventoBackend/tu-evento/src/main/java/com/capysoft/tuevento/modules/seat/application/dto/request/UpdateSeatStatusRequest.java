package com.capysoft.tuevento.modules.seat.application.dto.request;

import com.capysoft.tuevento.modules.seat.domain.model.SeatStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSeatStatusRequest {

    @NotNull(message = "El nuevo estado es obligatorio")
    private SeatStatus newStatus;

    private String reason;
}
