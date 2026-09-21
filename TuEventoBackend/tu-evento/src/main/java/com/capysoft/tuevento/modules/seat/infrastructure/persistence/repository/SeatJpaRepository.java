package com.capysoft.tuevento.modules.seat.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.seat.domain.model.SeatStatus;
import com.capysoft.tuevento.modules.seat.infrastructure.persistence.entity.SeatEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface SeatJpaRepository extends JpaRepository<SeatEntity, Integer> {
    List<SeatEntity> findAllBySeatBlockId(Integer seatBlockId);
    List<SeatEntity> findAllByEventSectionId(Integer eventSectionId);
    boolean existsByEventSectionIdAndCode(Integer eventSectionId, String code);
    List<SeatEntity> findAllByStatusAndReservedUntilBefore(SeatStatus status, LocalDateTime dateTime);
}
