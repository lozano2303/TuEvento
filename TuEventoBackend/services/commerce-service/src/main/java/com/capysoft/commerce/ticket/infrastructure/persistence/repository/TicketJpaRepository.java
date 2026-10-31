package com.capysoft.commerce.ticket.infrastructure.persistence.repository;

import com.capysoft.commerce.ticket.infrastructure.persistence.entity.TicketEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para TicketEntity.
 */
@Repository
public interface TicketJpaRepository extends JpaRepository<TicketEntity, Long> {
    List<TicketEntity> findByOrderId(Long orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TicketEntity t WHERE t.ticketId = :ticketId")
    Optional<TicketEntity> findByIdForUpdate(@Param("ticketId") Long ticketId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TicketEntity t WHERE t.orderId = :orderId ORDER BY t.ticketId")
    List<TicketEntity> findByOrderIdForUpdate(@Param("orderId") Long orderId);

    List<TicketEntity> findByUserId(Long userId);
    Optional<TicketEntity> findByCode(String code);
    long countByEventIdAndStatusIn(Long eventId, java.util.Collection<com.capysoft.commerce.ticket.domain.model.TicketStatus> statuses);
}
