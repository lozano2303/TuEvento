package com.capysoft.tuevento.modules.ticket.application.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request para crear una orden con tickets.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {
    
    @NotNull(message = "Event ID is required")
    private Long eventId;
    
    @NotEmpty(message = "At least one seat is required")
    private List<Integer> seatIds;
}
