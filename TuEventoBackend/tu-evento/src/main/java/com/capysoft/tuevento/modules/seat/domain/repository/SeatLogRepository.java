package com.capysoft.tuevento.modules.seat.domain.repository;

import com.capysoft.tuevento.modules.seat.domain.model.SeatLog;

import java.util.List;

public interface SeatLogRepository {
    List<SeatLog> findAllBySeatId(Integer seatId);
    SeatLog save(SeatLog seatLog);
}
