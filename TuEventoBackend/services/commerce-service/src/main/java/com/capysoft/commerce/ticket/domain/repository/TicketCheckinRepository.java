package com.capysoft.commerce.ticket.domain.repository;

import com.capysoft.commerce.ticket.domain.model.TicketCheckin;

import java.util.Optional;

/**
 * Repositorio del dominio para TicketCheckin.
 */
public interface TicketCheckinRepository {
    TicketCheckin save(TicketCheckin checkin);
    Optional<TicketCheckin> findByTicketId(Long ticketId);
    boolean existsByTicketId(Long ticketId);
}
