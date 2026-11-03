package com.capysoft.commerce.ticket.domain.model;

/**
 * Estados posibles de un ticket.
 */
public enum TicketStatus {
    PENDING,
    PROCESSING,
    PAID,
    REFUNDED,
    USED,
    CANCELLED
}
