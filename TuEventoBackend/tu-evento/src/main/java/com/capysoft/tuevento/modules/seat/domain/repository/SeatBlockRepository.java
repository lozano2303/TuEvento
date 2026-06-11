package com.capysoft.tuevento.modules.seat.domain.repository;

import com.capysoft.tuevento.modules.seat.domain.model.SeatBlock;

import java.util.List;
import java.util.Optional;

public interface SeatBlockRepository {
    List<SeatBlock> findAllByEventSectionId(Integer eventSectionId);
    Optional<SeatBlock> findById(Integer seatBlockId);
    boolean existsByEventSectionIdAndName(Integer eventSectionId, String name);
    SeatBlock save(SeatBlock seatBlock);
    void deleteById(Integer seatBlockId);
}
