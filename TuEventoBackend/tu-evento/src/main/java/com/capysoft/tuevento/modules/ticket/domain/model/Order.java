package com.capysoft.tuevento.modules.ticket.domain.model;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Aggregate root del dominio de órdenes.
 * Representa una orden de compra de tickets para un evento.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    private Long orderId;
    private Long userId;
    private Long eventId;
    private Money totalAmount;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    
    /**
     * Transiciona la orden a un nuevo estado validando las reglas del dominio.
     */
    public void transitionTo(OrderStatus newStatus) {
        if (!status.canTransitionTo(newStatus)) {
            throw new InvalidOrderStatusTransitionException(
                String.format("Cannot transition order %d from %s to %s", 
                    orderId, status, newStatus)
            );
        }
        this.status = newStatus;
    }
    
    /**
     * Marca la orden como pagada.
     */
    public void markAsPaid() {
        transitionTo(OrderStatus.PAID);
    }
    
    /**
     * Cancela la orden.
     */
    public void cancel() {
        transitionTo(OrderStatus.CANCELLED);
    }
    
    /**
     * Transiciona la orden a estado PAYMENT_PENDING.
     */
    public void markAsPaymentPending() {
        transitionTo(OrderStatus.PAYMENT_PENDING);
    }
    
    /**
     * Reembolsa la orden.
     */
    public void refund() {
        transitionTo(OrderStatus.REFUNDED);
    }
    
    /**
     * Marca la orden como usada.
     */
    public void markAsUsed() {
        transitionTo(OrderStatus.USED);
    }
}
