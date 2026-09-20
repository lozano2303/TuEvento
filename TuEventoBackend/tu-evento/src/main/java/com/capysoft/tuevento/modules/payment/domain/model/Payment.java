package com.capysoft.tuevento.modules.payment.domain.model;

import com.capysoft.tuevento.modules.ticket.domain.model.Money;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Aggregate root del dominio de pagos.
 * Representa un pago procesado a través de un gateway externo.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    private Long paymentId;
    private Long orderId;
    private PaymentGateway gateway;
    private String gatewayTransactionId; // providerPaymentId del gateway externo
    private PaymentStatus status;
    private Money amount;
    private PaymentMethod paymentMethod;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    
    /**
     * Transiciona el pago a un nuevo estado validando las reglas del dominio.
     */
    public void transitionTo(PaymentStatus newStatus) {
        if (!status.canTransitionTo(newStatus)) {
            throw new InvalidPaymentStatusTransitionException(
                String.format("Cannot transition payment %d from %s to %s", 
                    paymentId, status, newStatus)
            );
        }
        this.status = newStatus;
    }
    
    /**
     * Marca el pago como aprobado.
     */
    public void approve() {
        transitionTo(PaymentStatus.APPROVED);
        this.processedAt = LocalDateTime.now();
    }
    
    /**
     * Marca el pago como rechazado.
     */
    public void reject() {
        transitionTo(PaymentStatus.REJECTED);
        this.processedAt = LocalDateTime.now();
    }
    
    /**
     * Marca el pago como error.
     */
    public void markAsError() {
        transitionTo(PaymentStatus.ERROR);
        this.processedAt = LocalDateTime.now();
    }
    
    /**
     * Reembolsa el pago.
     */
    public void refund() {
        transitionTo(PaymentStatus.REFUNDED);
    }
}
