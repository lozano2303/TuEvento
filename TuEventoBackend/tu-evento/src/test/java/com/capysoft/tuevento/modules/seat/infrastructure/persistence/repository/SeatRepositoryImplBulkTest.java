package com.capysoft.tuevento.modules.seat.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.seat.domain.model.Seat;
import com.capysoft.tuevento.modules.seat.domain.model.SeatBlock;
import com.capysoft.tuevento.modules.seat.domain.model.SeatStatus;
import com.capysoft.tuevento.modules.seat.domain.model.SeatType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Operaciones masivas de sillas contra una base real (H2 en modo PostgreSQL del perfil test):
 * el SQL del batch insert y los DELETE por sección.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({SeatRepositoryImpl.class, SeatBlockRepositoryImpl.class})
@DisplayName("SeatRepositoryImpl — operaciones masivas")
class SeatRepositoryImplBulkTest {

    @Autowired SeatRepositoryImpl seatRepository;
    @Autowired SeatBlockRepositoryImpl seatBlockRepository;
    @Autowired SeatJpaRepository seatJpaRepository;

    private static List<Seat> seats(int blockId, int sectionId, int count) {
        List<Seat> seats = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            seats.add(Seat.builder()
                    .seatBlockId(blockId).eventSectionId(sectionId)
                    .code("S" + sectionId + "-" + i).row(1 + (i - 1) / 50).position(1 + (i - 1) % 50)
                    .type(SeatType.REGULAR).status(SeatStatus.AVAILABLE)
                    .build());
        }
        return seats;
    }

    @Test
    @DisplayName("insertAll guarda todas las sillas aunque ocupen varios lotes")
    void insertAllSpansSeveralBatches() {
        int count = SeatRepositoryImpl.INSERT_BATCH_SIZE * 2 + 500;
        seatRepository.insertAll(seats(1, 10, count));

        List<Seat> stored = seatRepository.findAllByEventSectionId(10);
        assertThat(stored).hasSize(count);
        assertThat(stored).allSatisfy(seat -> {
            assertThat(seat.getSeatId()).isNotNull();
            assertThat(seat.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
            assertThat(seat.getType()).isEqualTo(SeatType.REGULAR);
            assertThat(seat.getReservedBy()).isNull();
        });
    }

    @Test
    @DisplayName("insertAll conserva quién reservó y hasta cuándo")
    void insertAllKeepsReservationFields() {
        java.time.LocalDateTime until = java.time.LocalDateTime.of(2026, 10, 25, 20, 30);
        seatRepository.insertAll(List.of(Seat.builder()
                .seatBlockId(1).eventSectionId(30).code("R1").row(1).position(1)
                .type(SeatType.REGULAR).status(SeatStatus.RESERVED).reservedBy(77).reservedUntil(until)
                .build()));

        Seat stored = seatRepository.findAllByEventSectionId(30).get(0);
        assertThat(stored.getStatus()).isEqualTo(SeatStatus.RESERVED);
        assertThat(stored.getReservedBy()).isEqualTo(77);
        assertThat(stored.getReservedUntil()).isEqualTo(until);
    }

    @Test
    @DisplayName("insertAll con una lista vacía no hace nada")
    void insertAllWithNothing() {
        seatRepository.insertAll(List.of());
        assertThat(seatJpaRepository.count()).isZero();
    }

    @Test
    @DisplayName("existsByEventSectionIdAndStatusNot detecta una silla no disponible")
    void detectsTakenSeat() {
        seatRepository.insertAll(seats(1, 10, 20));
        assertThat(seatRepository.existsByEventSectionIdAndStatusNot(10, SeatStatus.AVAILABLE)).isFalse();

        Seat first = seatRepository.findAllByEventSectionId(10).get(0);
        seatRepository.save(Seat.builder()
                .seatId(first.getSeatId()).seatBlockId(1).eventSectionId(10).code(first.getCode())
                .row(first.getRow()).position(first.getPosition())
                .type(SeatType.REGULAR).status(SeatStatus.SOLD)
                .build());

        assertThat(seatRepository.existsByEventSectionIdAndStatusNot(10, SeatStatus.AVAILABLE)).isTrue();
    }

    @Test
    @DisplayName("deleteAllByEventSectionId borra solo las sillas y bloques de esa sección")
    void deletesOnlyTheGivenSection() {
        SeatBlock blockA = seatBlockRepository.save(SeatBlock.builder().eventSectionId(10).name("A").capacity(30).build());
        SeatBlock blockB = seatBlockRepository.save(SeatBlock.builder().eventSectionId(20).name("B").capacity(5).build());
        seatRepository.insertAll(seats(blockA.getSeatBlockId(), 10, 30));
        seatRepository.insertAll(seats(blockB.getSeatBlockId(), 20, 5));

        seatRepository.deleteAllByEventSectionId(10);
        seatBlockRepository.deleteAllByEventSectionId(10);

        assertThat(seatRepository.findAllByEventSectionId(10)).isEmpty();
        assertThat(seatRepository.findAllByEventSectionId(20)).hasSize(5);
        assertThat(seatBlockRepository.findAllByEventSectionId(10)).isEmpty();
        assertThat(seatBlockRepository.findAllByEventSectionId(20)).hasSize(1);
    }
}
