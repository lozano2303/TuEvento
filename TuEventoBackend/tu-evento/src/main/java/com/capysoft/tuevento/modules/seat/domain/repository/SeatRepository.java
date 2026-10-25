package com.capysoft.tuevento.modules.seat.domain.repository;

import com.capysoft.tuevento.modules.seat.domain.model.Seat;
import com.capysoft.tuevento.modules.seat.domain.model.SeatStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SeatRepository {
    List<Seat> findAllBySeatBlockId(Integer seatBlockId);
    List<Seat> findAllByEventSectionId(Integer eventSectionId);
    Optional<Seat> findById(Integer seatId);
    boolean existsByEventSectionIdAndCode(Integer eventSectionId, String code);
    Seat save(Seat seat);
    void deleteById(Integer seatId);

    /** true si la sección tiene alguna silla en un estado distinto del indicado (una sola consulta). */
    boolean existsByEventSectionIdAndStatusNot(Integer eventSectionId, SeatStatus status);

    /** Borra todas las sillas de la sección con una sola sentencia. */
    void deleteAllByEventSectionId(Integer eventSectionId);

    /** Inserta sillas nuevas por lotes. No devuelve los ids generados. */
    void insertAll(List<Seat> seats);
    List<Seat> findAllByStatusAndReservedUntilBefore(SeatStatus status, LocalDateTime dateTime);
}
