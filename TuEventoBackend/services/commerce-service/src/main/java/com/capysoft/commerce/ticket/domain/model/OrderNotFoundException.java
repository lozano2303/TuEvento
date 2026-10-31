package com.capysoft.commerce.ticket.domain.model;

/**
 * Excepción lanzada cuando no se encuentra una orden.
 */
public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(Long orderId) {
        super("Order not found: " + orderId);
    }
}
