package com.capysoft.tuevento.modules.ticket.domain.model;

/**
 * Excepción de dominio lanzada cuando se intenta una transición de estado inválida en Order.
 */
public class InvalidOrderStatusTransitionException extends RuntimeException {
    public InvalidOrderStatusTransitionException(String message) {
        super(message);
    }
}
