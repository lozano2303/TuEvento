package com.capysoft.tuevento.modules.ticket.domain.model;

/**
 * Excepción lanzada cuando no se encuentra un ticket.
 */
public class TicketNotFoundException extends RuntimeException {
    public TicketNotFoundException(Long ticketId) {
        super("Ticket not found: " + ticketId);
    }
    
    public TicketNotFoundException(String message) {
        super(message);
    }
}
