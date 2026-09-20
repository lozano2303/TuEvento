package com.capysoft.tuevento.modules.ticket.domain.model;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad del dominio que representa el check-in de un ticket.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketCheckin {
    private Long checkinId;
    private Long ticketId;
    private LocalDateTime checkinTime;
    private Long validatedBy; // userId del staff que validó
}
