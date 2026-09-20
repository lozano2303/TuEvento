package com.capysoft.tuevento.modules.ticket.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.ticket.infrastructure.persistence.entity.TicketEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para TicketEntity.
 */
@Repository
public interface TicketJpaRepository extends JpaRepository<TicketEntity, Long> {
    List<TicketEntity> findByOrderId(Long orderId);
    List<TicketEntity> findByUserId(Long userId);
    Optional<TicketEntity> findByCode(String code);
}
