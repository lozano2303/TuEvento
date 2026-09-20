package com.capysoft.tuevento.modules.ticket.domain.repository;

import com.capysoft.tuevento.modules.ticket.domain.model.TicketLog;

import java.util.List;

/**
 * Repositorio del dominio para TicketLog (auditoría).
 */
public interface TicketLogRepository {
    TicketLog save(TicketLog log);
    List<TicketLog> findByTicketId(Long ticketId);
}
