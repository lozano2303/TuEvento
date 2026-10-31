package com.capysoft.commerce.ticket.domain.repository;

import com.capysoft.commerce.ticket.domain.model.Ticket;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio del dominio para Ticket.
 */
public interface TicketRepository {
    Ticket save(Ticket ticket);
    List<Ticket> saveAll(List<Ticket> tickets);
    Optional<Ticket> findById(Long ticketId);
    List<Ticket> findByOrderId(Long orderId);
    /**
     * Igual que findById pero bloquea la fila hasta el fin de la transacción (SELECT ... FOR UPDATE).
     * Lo usan el escaneo/check-in y el reembolso para no pisarse sobre el mismo ticket.
     */
    Optional<Ticket> findByIdForUpdate(Long ticketId);
    /** Tickets de la orden bloqueados hasta el fin de la transacción, en orden de id (evita deadlocks). */
    List<Ticket> findByOrderIdForUpdate(Long orderId);
    List<Ticket> findByUserId(Long userId);
    Optional<Ticket> findByCode(String code);
    boolean existsById(Long ticketId);
    /** Entradas vendidas de un evento (pagadas o ya usadas). */
    long countSoldByEventId(Long eventId);
}
