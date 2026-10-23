package com.capysoft.tuevento.modules.seat.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.seat.domain.model.SeatLog;
import com.capysoft.tuevento.modules.seat.domain.repository.SeatLogRepository;
import com.capysoft.tuevento.modules.seat.infrastructure.persistence.entity.SeatLogEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class SeatLogRepositoryImpl implements SeatLogRepository {

    private final SeatLogJpaRepository jpaRepository;

    @Override
    public List<SeatLog> findAllBySeatId(Integer seatId) {
        return jpaRepository.findAllBySeatId(seatId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public SeatLog save(SeatLog seatLog) {
        SeatLogEntity entity = SeatLogEntity.builder()
                .seatLogId(seatLog.getSeatLogId())
                .seatId(seatLog.getSeatId())
                .oldStatus(seatLog.getOldStatus())
                .newStatus(seatLog.getNewStatus())
                .changedAt(seatLog.getChangedAt())
                .changedBy(seatLog.getChangedBy())
                .reason(seatLog.getReason())
                .build();
        return toDomain(jpaRepository.save(entity));
    }

    private SeatLog toDomain(SeatLogEntity entity) {
        return SeatLog.builder()
                .seatLogId(entity.getSeatLogId())
                .seatId(entity.getSeatId())
                .oldStatus(entity.getOldStatus())
                .newStatus(entity.getNewStatus())
                .changedAt(entity.getChangedAt())
                .changedBy(entity.getChangedBy())
                .reason(entity.getReason())
                .build();
    }
}
