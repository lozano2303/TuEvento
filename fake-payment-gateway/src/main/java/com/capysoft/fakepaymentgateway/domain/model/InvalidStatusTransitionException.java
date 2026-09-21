package com.capysoft.fakepaymentgateway.domain.model;

/**
 * Excepción de dominio lanzada cuando se intenta una transición de estado inválida.
 */
public class InvalidStatusTransitionException extends RuntimeException {
    public InvalidStatusTransitionException(String message) {
        super(message);
    }
}
