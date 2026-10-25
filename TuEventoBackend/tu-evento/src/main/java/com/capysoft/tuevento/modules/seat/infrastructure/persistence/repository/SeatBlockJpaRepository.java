package com.capysoft.tuevento.modules.seat.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.seat.infrastructure.persistence.entity.SeatBlockEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SeatBlockJpaRepository extends JpaRepository<SeatBlockEntity, Integer> {
    List<SeatBlockEntity> findAllByEventSectionId(Integer eventSectionId);
    boolean existsByEventSectionIdAndName(Integer eventSectionId, String name);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM SeatBlockEntity b WHERE b.eventSectionId = :eventSectionId")
    int deleteAllInSection(@Param("eventSectionId") Integer eventSectionId);
}
