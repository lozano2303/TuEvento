package com.capysoft.commerce.ticket.infrastructure.persistence.repository;

import com.capysoft.commerce.ticket.infrastructure.persistence.entity.TicketLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio JPA para TicketLogEntity.
 */
@Repository
public interface TicketLogJpaRepository extends JpaRepository<TicketLogEntity, Long> {
    List<TicketLogEntity> findByTicketId(Long ticketId);
}
