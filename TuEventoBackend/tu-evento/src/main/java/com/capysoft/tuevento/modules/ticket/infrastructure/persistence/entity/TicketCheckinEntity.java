package com.capysoft.tuevento.modules.ticket.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad JPA para TicketCheckin.
 */
@Entity
@Table(name = "ticket_checkin")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketCheckinEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "checkin_id")
    private Long checkinId;
    
    @Column(name = "ticket_id", nullable = false, unique = true)
    private Long ticketId;
    
    @Column(name = "checkin_time", nullable = false)
    private LocalDateTime checkinTime;
    
    @Column(name = "validated_by", nullable = false)
    private Long validatedBy;
}
