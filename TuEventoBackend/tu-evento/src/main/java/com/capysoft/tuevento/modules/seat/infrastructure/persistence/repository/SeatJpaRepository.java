package com.capysoft.tuevento.modules.seat.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.event.domain.model.EventStatus;
import com.capysoft.tuevento.modules.seat.domain.model.EventSeatSummary;
import com.capysoft.tuevento.modules.seat.domain.model.SeatStatus;
import com.capysoft.tuevento.modules.seat.infrastructure.persistence.entity.SeatEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface SeatJpaRepository extends JpaRepository<SeatEntity, Integer> {
    List<SeatEntity> findAllBySeatBlockId(Integer seatBlockId);
    List<SeatEntity> findAllByEventSectionId(Integer eventSectionId);
    boolean existsByEventSectionIdAndCode(Integer eventSectionId, String code);
    List<SeatEntity> findAllByStatusAndReservedUntilBefore(SeatStatus status, LocalDateTime dateTime);

    boolean existsByEventSectionIdAndStatusIn(Integer eventSectionId, Collection<SeatStatus> statuses);

    // DELETE explícito: el deleteAllBy derivado de Spring Data carga y borra las filas una a una
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM SeatEntity s WHERE s.eventSectionId = :eventSectionId")
    int deleteAllInSection(@Param("eventSectionId") Integer eventSectionId);

    /** Ganancia potencial: cada silla que no es de cortesía vale el precio de su sección. */
    @Query("""
            SELECT new com.capysoft.tuevento.modules.seat.domain.model.EventSeatSummary(
                e.eventId,
                e.eventName,
                COALESCE(SUM(CASE WHEN s.status <> :courtesy THEN es.price ELSE 0 END), 0),
                SUM(CASE WHEN s.status = :courtesy THEN 1L ELSE 0L END),
                COUNT(s.seatId)
            )
            FROM SeatEntity s
            JOIN EventSectionEntity es ON es.eventSectionId = s.eventSectionId
            JOIN EventEntity e ON e.eventId = es.eventId
            WHERE e.userId = :organizerId
              AND e.status <> :cancelled
            GROUP BY e.eventId, e.eventName
            """)
    List<EventSeatSummary> summarizeSeatsByOrganizer(@Param("organizerId") Long organizerId,
                                                     @Param("courtesy") SeatStatus courtesy,
                                                     @Param("cancelled") EventStatus cancelled);
}
