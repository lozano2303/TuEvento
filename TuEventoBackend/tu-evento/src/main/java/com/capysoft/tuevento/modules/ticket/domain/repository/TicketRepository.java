package com.capysoft.tuevento.modules.ticket.domain.repository;

import com.capysoft.tuevento.modules.ticket.domain.model.Ticket;

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
    List<Ticket> findByUserId(Long userId);
    Optional<Ticket> findByCode(String code);
    boolean existsById(Long ticketId);
}
