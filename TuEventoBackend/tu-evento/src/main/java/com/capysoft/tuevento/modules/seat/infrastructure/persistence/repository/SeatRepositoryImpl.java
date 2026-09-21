package com.capysoft.tuevento.modules.seat.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.seat.domain.model.Seat;
import com.capysoft.tuevento.modules.seat.domain.model.SeatStatus;
import com.capysoft.tuevento.modules.seat.domain.repository.SeatRepository;
import com.capysoft.tuevento.modules.seat.infrastructure.persistence.entity.SeatEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SeatRepositoryImpl implements SeatRepository {

    private final SeatJpaRepository jpaRepository;

    @Override
    public List<Seat> findAllBySeatBlockId(Integer seatBlockId) {
        return jpaRepository.findAllBySeatBlockId(seatBlockId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Seat> findAllByEventSectionId(Integer eventSectionId) {
        return jpaRepository.findAllByEventSectionId(eventSectionId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<Seat> findById(Integer seatId) {
        return jpaRepository.findById(seatId).map(this::toDomain);
    }

    @Override
    public boolean existsByEventSectionIdAndCode(Integer eventSectionId, String code) {
        return jpaRepository.existsByEventSectionIdAndCode(eventSectionId, code);
    }

    @Override
    public Seat save(Seat seat) {
        SeatEntity entity = SeatEntity.builder()
                .seatId(seat.getSeatId())
                .seatBlockId(seat.getSeatBlockId())
                .eventSectionId(seat.getEventSectionId())
                .code(seat.getCode())
                .row(seat.getRow())
                .position(seat.getPosition())
                .type(seat.getType())
                .status(seat.getStatus())
                .reservedBy(seat.getReservedBy())
                .reservedUntil(seat.getReservedUntil())
                .build();
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public void deleteById(Integer seatId) {
        jpaRepository.deleteById(seatId);
    }

    @Override
    public List<Seat> findAllByStatusAndReservedUntilBefore(SeatStatus status, LocalDateTime dateTime) {
        return jpaRepository.findAllByStatusAndReservedUntilBefore(status, dateTime).stream()
                .map(this::toDomain)
                .toList();
    }

    private Seat toDomain(SeatEntity entity) {
        return Seat.builder()
                .seatId(entity.getSeatId())
                .seatBlockId(entity.getSeatBlockId())
                .eventSectionId(entity.getEventSectionId())
                .code(entity.getCode())
                .row(entity.getRow())
                .position(entity.getPosition())
                .type(entity.getType())
                .status(entity.getStatus())
                .reservedBy(entity.getReservedBy())
                .reservedUntil(entity.getReservedUntil())
                .build();
    }
}
