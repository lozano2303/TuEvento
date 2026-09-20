package com.capysoft.tuevento.modules.payment.domain.model;

/**
 * Excepción lanzada cuando se intenta una transición de estado inválida en Payment.
 */
public class InvalidPaymentStatusTransitionException extends RuntimeException {
    public InvalidPaymentStatusTransitionException(String message) {
        super(message);
    }
}
