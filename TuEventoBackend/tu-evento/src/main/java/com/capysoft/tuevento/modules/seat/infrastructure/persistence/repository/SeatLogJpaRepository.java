package com.capysoft.tuevento.modules.seat.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.seat.infrastructure.persistence.entity.SeatLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeatLogJpaRepository extends JpaRepository<SeatLogEntity, Integer> {
    List<SeatLogEntity> findAllBySeatId(Integer seatId);
}
