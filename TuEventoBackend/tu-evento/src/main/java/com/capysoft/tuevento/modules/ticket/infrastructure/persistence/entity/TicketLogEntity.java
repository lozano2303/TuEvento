package com.capysoft.tuevento.modules.ticket.infrastructure.persistence.entity;

import com.capysoft.tuevento.modules.ticket.domain.model.TicketStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad JPA para TicketLog (auditoría inmutable).
 */
@Entity
@Table(name = "ticket_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketLogEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_log_id")
    private Long ticketLogId;
    
    @Column(name = "ticket_id", nullable = false)
    private Long ticketId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", length = 20)
    private TicketStatus oldStatus;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 20)
    private TicketStatus newStatus;
    
    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;
    
    @Column(name = "changed_by", length = 100)
    private String changedBy;
    
    @Column(name = "reason", length = 255)
    private String reason;
}
