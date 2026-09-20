package com.capysoft.tuevento.modules.ticket.domain.repository;

import com.capysoft.tuevento.modules.ticket.domain.model.SeatTicket;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio del dominio para SeatTicket.
 */
public interface SeatTicketRepository {
    SeatTicket save(SeatTicket seatTicket);
    List<SeatTicket> saveAll(List<SeatTicket> seatTickets);
    Optional<SeatTicket> findById(Long seatTicketId);
    List<SeatTicket> findByTicketId(Long ticketId);
    Optional<SeatTicket> findBySeatIdAndTicketId(Integer seatId, Long ticketId);
}
