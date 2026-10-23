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
    List<Seat> findAllByStatusAndReservedUntilBefore(SeatStatus status, LocalDateTime dateTime);
}
