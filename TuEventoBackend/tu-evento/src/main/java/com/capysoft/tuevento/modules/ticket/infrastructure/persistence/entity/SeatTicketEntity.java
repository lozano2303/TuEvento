package com.capysoft.tuevento.modules.ticket.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Entidad JPA para SeatTicket (relación N:M entre Seat y Ticket con precio snapshot).
 */
@Entity
@Table(name = "seat_ticket")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatTicketEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seat_ticket_id")
    private Long seatTicketId;
    
    @Column(name = "seat_id", nullable = false)
    private Integer seatId;
    
    @Column(name = "ticket_id", nullable = false)
    private Long ticketId;
    
    @Column(name = "price", nullable = false, precision = 19, scale = 4)
    private BigDecimal price;
}
