package com.capysoft.fakepaymentgateway.domain.model;

import com.capysoft.fakepaymentgateway.domain.event.PaymentStatusChanged;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate root del dominio de pagos.
 * Encapsula la máquina de estados y las reglas de negocio.
 */
public class Payment {
    private String paymentId;
    private String externalReference;  // Referencia del backend principal
    private PaymentStatus status;
    private Money money;
    private PaymentMethod paymentMethod;
    private Instant createdAt;
    private Instant updatedAt;
    private String webhookEventId;  // UUID estable por transición
    
    // Constructor público para JPA y reconstrucción
    public Payment() {}
    
    /**
     * Crea un nuevo pago en estado PENDING.
     */
    public static Payment create(
        String externalReference,
        Money money,
        PaymentMethod paymentMethod
    ) {
        Payment payment = new Payment();
        payment.paymentId = "fake_" + UUID.randomUUID().toString();
        payment.externalReference = externalReference;
        payment.status = PaymentStatus.PENDING;
        payment.money = money;
        payment.paymentMethod = paymentMethod;
        payment.createdAt = Instant.now();
        payment.updatedAt = Instant.now();
        payment.webhookEventId = UUID.randomUUID().toString();  // Primera transición
        
        return payment;
    }
    
    /**
     * Marca el pago como en procesamiento.
     */
    public PaymentStatusChanged markAsProcessing() {
        transitionTo(PaymentStatus.PROCESSING);
        return createStatusChangedEvent();
    }
    
    /**
     * Aprueba el pago.
     */
    public PaymentStatusChanged approve() {
        transitionTo(PaymentStatus.APPROVED);
        return createStatusChangedEvent();
    }
    
    /**
     * Declina el pago.
     */
    public PaymentStatusChanged decline() {
        transitionTo(PaymentStatus.DECLINED);
        return createStatusChangedEvent();
    }
    
    /**
     * Marca el pago como fallido.
     */
    public PaymentStatusChanged fail() {
        transitionTo(PaymentStatus.FAILED);
        return createStatusChangedEvent();
    }
    
    /**
     * Cancela el pago.
     */
    public PaymentStatusChanged cancel() {
        transitionTo(PaymentStatus.CANCELLED);
        return createStatusChangedEvent();
    }

    /**
     * Reembolsa el pago. Transición válida: APPROVED → REFUNDED.
     */
    public PaymentStatusChanged refund() {
        transitionTo(PaymentStatus.REFUNDED);
        return createStatusChangedEvent();
    }
    
    /**
     * Realiza una transición de estado validando las reglas del dominio.
     */
    private void transitionTo(PaymentStatus newStatus) {
        if (!status.canTransitionTo(newStatus)) {
            throw new InvalidStatusTransitionException(
                String.format("Cannot transition from %s to %s", status, newStatus)
            );
        }
        
        this.status = newStatus;
        this.updatedAt = Instant.now();
        this.webhookEventId = UUID.randomUUID().toString();  // Nuevo eventId para esta transición
    }
    
    /**
     * Crea un evento de cambio de estado.
     */
    private PaymentStatusChanged createStatusChangedEvent() {
        return new PaymentStatusChanged(
            webhookEventId,
            paymentId,
            status,
            money,
            updatedAt
        );
    }
    
    // Getters
    public String getPaymentId() { return paymentId; }
    public String getExternalReference() { return externalReference; }
    public PaymentStatus getStatus() { return status; }
    public Money getMoney() { return money; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getWebhookEventId() { return webhookEventId; }
    
    // Setters para reconstrucción desde JPA
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    public void setExternalReference(String externalReference) { this.externalReference = externalReference; }
    public void setStatus(PaymentStatus status) { this.status = status; }
    public void setMoney(Money money) { this.money = money; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public void setWebhookEventId(String webhookEventId) { this.webhookEventId = webhookEventId; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Payment payment = (Payment) o;
        return Objects.equals(paymentId, payment.paymentId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(paymentId);
    }
}
