package com.capysoft.commerce.payment.domain.model;

/**
 * Excepción lanzada cuando no se encuentra un pago.
 */
public class PaymentNotFoundException extends RuntimeException {
    public PaymentNotFoundException(Long paymentId) {
        super("Payment not found: " + paymentId);
    }
    
    public PaymentNotFoundException(String message) {
        super(message);
    }
}
