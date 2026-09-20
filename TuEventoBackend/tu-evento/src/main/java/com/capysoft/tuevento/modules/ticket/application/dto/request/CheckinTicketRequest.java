package com.capysoft.tuevento.modules.ticket.application.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Request para hacer check-in de un ticket.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CheckinTicketRequest {
    
    @NotNull(message = "Validated by user ID is required")
    private Long validatedBy;
}
