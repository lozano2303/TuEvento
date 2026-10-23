package com.capysoft.tuevento.modules.ticket.domain.model;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Aggregate root del dominio de tickets.
 * Representa un ticket individual asociado a una orden.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ticket {
    private Long ticketId;
    private Long eventId;
    private Long userId;
    private Long orderId;
    private String code;
    private String qrCode;
    private TicketStatus status;
    private LocalDateTime expirationDate;
    private Money totalPrice;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    
    /**
     * Marca el ticket como pagado.
     */
    public void markAsPaid() {
        this.status = TicketStatus.PAID;
    }
    
    /**
     * Marca el ticket como usado.
     */
    public void markAsUsed() {
        this.status = TicketStatus.USED;
    }
    
    /**
     * Marca el ticket como reembolsado.
     */
    public void markAsRefunded() {
        this.status = TicketStatus.REFUNDED;
    }
    
    /**
     * Marca el ticket como cancelado.
     */
    public void cancel() {
        this.status = TicketStatus.CANCELLED;
    }
    
    /**
     * Verifica si el ticket puede hacer check-in.
     */
    public boolean canCheckin() {
        return status == TicketStatus.PAID && 
               expirationDate != null && 
               LocalDateTime.now().isBefore(expirationDate);
    }
}
