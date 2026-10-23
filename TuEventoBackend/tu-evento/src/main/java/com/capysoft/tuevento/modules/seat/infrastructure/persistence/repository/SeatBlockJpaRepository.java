package com.capysoft.tuevento.modules.seat.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.seat.infrastructure.persistence.entity.SeatBlockEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeatBlockJpaRepository extends JpaRepository<SeatBlockEntity, Integer> {
    List<SeatBlockEntity> findAllByEventSectionId(Integer eventSectionId);
    boolean existsByEventSectionIdAndName(Integer eventSectionId, String name);
}
