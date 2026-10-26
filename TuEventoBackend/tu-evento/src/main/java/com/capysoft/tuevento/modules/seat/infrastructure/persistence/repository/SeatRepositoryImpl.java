package com.capysoft.tuevento.modules.seat.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.event.domain.model.EventStatus;
import com.capysoft.tuevento.modules.seat.domain.model.EventSeatSummary;
import com.capysoft.tuevento.modules.seat.domain.model.Seat;
import com.capysoft.tuevento.modules.seat.domain.model.SeatStatus;
import com.capysoft.tuevento.modules.seat.domain.repository.SeatRepository;
import com.capysoft.tuevento.modules.seat.infrastructure.persistence.entity.SeatEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SeatRepositoryImpl implements SeatRepository {

    /** Filas por lote en insertAll: equilibrio entre idas a la base y tamaño de cada sentencia. */
    static final int INSERT_BATCH_SIZE = 1000;

    private static final String INSERT_SQL =
            "INSERT INTO seat (seat_block_id, event_section_id, code, row, position, type, status, reserved_by, reserved_until) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private final SeatJpaRepository jpaRepository;
    private final JdbcTemplate jdbcTemplate;

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
    public Optional<Seat> findByIdForUpdate(Integer seatId) {
        return jpaRepository.findByIdForUpdate(seatId).map(this::toDomain);
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

    @Override
    public List<Seat> findExpiredReservationsWithoutPendingPayment(LocalDateTime now) {
        return jpaRepository.findExpiredReservationsWithoutPendingPayment(now).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public boolean existsByEventSectionIdAndStatusIn(Integer eventSectionId, Collection<SeatStatus> statuses) {
        return jpaRepository.existsByEventSectionIdAndStatusIn(eventSectionId, statuses);
    }

    @Override
    public List<EventSeatSummary> summarizeSeatsByOrganizer(Long organizerId) {
        return jpaRepository.summarizeSeatsByOrganizer(organizerId, SeatStatus.COURTESY, EventStatus.CANCELLED);
    }

    @Override
    public long countByEventSectionId(Integer eventSectionId) {
        return jpaRepository.countByEventSectionId(eventSectionId);
    }

    @Override
    public void deleteAllByEventSectionId(Integer eventSectionId) {
        jpaRepository.deleteAllInSection(eventSectionId);
    }

    /**
     * Inserción por lotes con JDBC. Las sillas usan ids IDENTITY, con los que Hibernate no
     * agrupa inserts (necesita el id de cada fila al guardarla); aquí no hacen falta los ids,
     * así que cada lote viaja en una sola ida a la base. Con reWriteBatchedInserts el driver
     * de PostgreSQL además lo convierte en un único INSERT multi-fila.
     */
    @Override
    public void insertAll(List<Seat> seats) {
        if (seats == null || seats.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate(INSERT_SQL, seats, INSERT_BATCH_SIZE, (ps, seat) -> {
            ps.setInt(1, seat.getSeatBlockId());
            ps.setInt(2, seat.getEventSectionId());
            ps.setString(3, seat.getCode());
            ps.setInt(4, seat.getRow());
            ps.setInt(5, seat.getPosition());
            ps.setString(6, seat.getType().name());
            ps.setString(7, seat.getStatus().name());
            if (seat.getReservedBy() != null) {
                ps.setInt(8, seat.getReservedBy());
            } else {
                ps.setNull(8, Types.INTEGER);
            }
            if (seat.getReservedUntil() != null) {
                ps.setTimestamp(9, Timestamp.valueOf(seat.getReservedUntil()));
            } else {
                ps.setNull(9, Types.TIMESTAMP);
            }
        });
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
