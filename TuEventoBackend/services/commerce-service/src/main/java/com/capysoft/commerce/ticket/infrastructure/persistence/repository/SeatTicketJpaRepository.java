package com.capysoft.commerce.ticket.infrastructure.persistence.repository;

import com.capysoft.commerce.ticket.domain.model.OrderStatus;
import com.capysoft.commerce.ticket.infrastructure.persistence.entity.SeatTicketEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para SeatTicketEntity.
 */
@Repository
public interface SeatTicketJpaRepository extends JpaRepository<SeatTicketEntity, Long> {
    List<SeatTicketEntity> findByTicketId(Long ticketId);
    Optional<SeatTicketEntity> findBySeatIdAndTicketId(Integer seatId, Long ticketId);

    /** Sillas de {@code seatIds} que tienen un ticket en una orden con el estado dado. */
    @Query("""
            SELECT DISTINCT st.seatId FROM SeatTicketEntity st, TicketEntity t, OrderEntity o
            WHERE t.ticketId = st.ticketId AND o.orderId = t.orderId
              AND o.status = :status AND st.seatId IN :seatIds
            """)
    List<Integer> findSeatIdsByOrderStatus(@Param("seatIds") Collection<Integer> seatIds,
                                           @Param("status") OrderStatus status);
}
