package com.capysoft.tuevento.modules.ticket.domain.model;

import lombok.*;

import java.math.BigDecimal;

/**
 * Entidad del dominio que representa la relación entre un ticket y una silla.
 * Contiene el precio snapshot al momento de la compra.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatTicket {
    private Long seatTicketId;
    private Integer seatId;
    private Long ticketId;
    private BigDecimal price; // Precio congelado al momento de la compra
}
