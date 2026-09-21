package com.capysoft.tuevento.modules.ticket.domain.model;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Evento de auditoría inmutable para cambios de estado en tickets.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketLog {
    private Long ticketLogId;
    private Long ticketId;
    private TicketStatus oldStatus;
    private TicketStatus newStatus;
    private LocalDateTime changedAt;
    private String changedBy;
    private String reason;
}
