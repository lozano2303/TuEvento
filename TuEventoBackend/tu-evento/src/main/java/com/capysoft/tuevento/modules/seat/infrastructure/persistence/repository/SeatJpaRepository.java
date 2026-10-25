package com.capysoft.tuevento.modules.seat.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.seat.domain.model.SeatStatus;
import com.capysoft.tuevento.modules.seat.infrastructure.persistence.entity.SeatEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SeatJpaRepository extends JpaRepository<SeatEntity, Integer> {
    List<SeatEntity> findAllBySeatBlockId(Integer seatBlockId);
    List<SeatEntity> findAllByEventSectionId(Integer eventSectionId);
    boolean existsByEventSectionIdAndCode(Integer eventSectionId, String code);
    List<SeatEntity> findAllByStatusAndReservedUntilBefore(SeatStatus status, LocalDateTime dateTime);

    boolean existsByEventSectionIdAndStatusNot(Integer eventSectionId, SeatStatus status);

    // DELETE explícito: el deleteAllBy derivado de Spring Data carga y borra las filas una a una
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM SeatEntity s WHERE s.eventSectionId = :eventSectionId")
    int deleteAllInSection(@Param("eventSectionId") Integer eventSectionId);
}
