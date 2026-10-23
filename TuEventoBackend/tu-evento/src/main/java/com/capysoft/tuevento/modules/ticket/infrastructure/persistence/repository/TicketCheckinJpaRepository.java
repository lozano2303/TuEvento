package com.capysoft.tuevento.modules.ticket.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.ticket.infrastructure.persistence.entity.TicketCheckinEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA para TicketCheckinEntity.
 */
@Repository
public interface TicketCheckinJpaRepository extends JpaRepository<TicketCheckinEntity, Long> {
    Optional<TicketCheckinEntity> findByTicketId(Long ticketId);
    boolean existsByTicketId(Long ticketId);
}
