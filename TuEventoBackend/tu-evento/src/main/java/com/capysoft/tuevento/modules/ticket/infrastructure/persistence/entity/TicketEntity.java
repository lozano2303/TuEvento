package com.capysoft.tuevento.modules.ticket.infrastructure.persistence.entity;

import com.capysoft.tuevento.modules.ticket.domain.model.TicketStatus;
import com.capysoft.tuevento.shared.infrastructure.persistence.JpaAuditingEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad JPA para Ticket.
 */
@Entity
@Table(name = "ticket")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketEntity extends JpaAuditingEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_id")
    private Long ticketId;
    
    @Column(name = "event_id", nullable = false)
    private Long eventId;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "order_id", nullable = false)
    private Long orderId;
    
    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;
    
    @Column(name = "qr_code", nullable = false, length = 500)
    private String qrCode;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TicketStatus status;
    
    @Column(name = "expiration_date")
    private LocalDateTime expirationDate;
    
    @Column(name = "total_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalPrice;
    
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;
}
