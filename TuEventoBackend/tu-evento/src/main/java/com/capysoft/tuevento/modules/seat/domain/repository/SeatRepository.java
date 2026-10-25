package com.capysoft.tuevento.modules.seat.domain.repository;

import com.capysoft.tuevento.modules.seat.domain.model.EventSeatSummary;
import com.capysoft.tuevento.modules.seat.domain.model.Seat;
import com.capysoft.tuevento.modules.seat.domain.model.SeatStatus;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SeatRepository {
    List<Seat> findAllBySeatBlockId(Integer seatBlockId);
    List<Seat> findAllByEventSectionId(Integer eventSectionId);
    Optional<Seat> findById(Integer seatId);
    boolean existsByEventSectionIdAndCode(Integer eventSectionId, String code);
    Seat save(Seat seat);
    void deleteById(Integer seatId);

    /** true si la sección tiene alguna silla en alguno de los estados indicados (una sola consulta). */
    boolean existsByEventSectionIdAndStatusIn(Integer eventSectionId, Collection<SeatStatus> statuses);

    /** Borra todas las sillas de la sección con una sola sentencia. */
    void deleteAllByEventSectionId(Integer eventSectionId);

    /** Inserta sillas nuevas por lotes. No devuelve los ids generados. */
    void insertAll(List<Seat> seats);
    List<Seat> findAllByStatusAndReservedUntilBefore(SeatStatus status, LocalDateTime dateTime);

    /** Sillas y ganancia potencial de cada evento no cancelado del organizador, en una sola consulta. */
    List<EventSeatSummary> summarizeSeatsByOrganizer(Long organizerId);
}
