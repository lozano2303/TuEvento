package com.capysoft.tuevento.modules.ticket.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.ticket.infrastructure.persistence.entity.SeatTicketEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para SeatTicketEntity.
 */
@Repository
public interface SeatTicketJpaRepository extends JpaRepository<SeatTicketEntity, Long> {
    List<SeatTicketEntity> findByTicketId(Long ticketId);
    Optional<SeatTicketEntity> findBySeatIdAndTicketId(Integer seatId, Long ticketId);
}
