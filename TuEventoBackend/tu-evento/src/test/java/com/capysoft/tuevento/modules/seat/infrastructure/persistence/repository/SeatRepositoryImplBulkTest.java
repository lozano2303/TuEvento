package com.capysoft.tuevento.modules.seat.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.seat.domain.model.EventSeatSummary;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
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
    @Autowired JdbcTemplate jdbcTemplate;

    private static final EnumSet<SeatStatus> TAKEN = EnumSet.of(SeatStatus.RESERVED, SeatStatus.SOLD);

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
    @DisplayName("existsByEventSectionIdAndStatusIn detecta una silla vendida, pero no una cortesía")
    void detectsTakenSeat() {
        List<Seat> section = new ArrayList<>(seats(1, 10, 20));
        section.add(Seat.builder().seatBlockId(1).eventSectionId(10).code("C1").row(9).position(1)
                .type(SeatType.COURTESY).status(SeatStatus.COURTESY).build());
        seatRepository.insertAll(section);
        assertThat(seatRepository.existsByEventSectionIdAndStatusIn(10, TAKEN)).isFalse();

        Seat first = seatRepository.findAllByEventSectionId(10).get(0);
        seatRepository.save(Seat.builder()
                .seatId(first.getSeatId()).seatBlockId(1).eventSectionId(10).code(first.getCode())
                .row(first.getRow()).position(first.getPosition())
                .type(SeatType.REGULAR).status(SeatStatus.SOLD)
                .build());

        assertThat(seatRepository.existsByEventSectionIdAndStatusIn(10, TAKEN)).isTrue();
    }

    private void insertEvent(long eventId, long userId, String name, String status) {
        jdbcTemplate.update("INSERT INTO event (event_id, user_id, site_id, event_name, description, start_date, "
                        + "finish_date, status, is_public, available_seats, created_at, updated_at) "
                        + "VALUES (?, ?, 1, ?, 'd', DATE '2026-12-01', DATE '2026-12-02', ?, true, 100, "
                        + "CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                eventId, userId, name, status);
    }

    private void insertSection(int sectionId, long eventId, String price) {
        jdbcTemplate.update("INSERT INTO event_section (event_section_id, event_id, section_type_id, capacity, "
                + "available_seats, price, is_active) VALUES (?, ?, 1, 0, 0, ?, true)",
                sectionId, eventId, new BigDecimal(price));
    }

    private static Seat courtesy(int sectionId, int position) {
        return Seat.builder().seatBlockId(1).eventSectionId(sectionId).code("C" + position).row(99).position(position)
                .type(SeatType.COURTESY).status(SeatStatus.COURTESY).build();
    }

    @Test
    @DisplayName("summarizeSeatsByOrganizer suma el precio de las sillas vendibles por evento y cuenta las cortesías")
    void summarizesPotentialEarningsPerEvent() {
        insertEvent(1, 7, "Concierto", "PUBLISHED");
        insertEvent(2, 7, "Cancelado", "CANCELLED");
        insertEvent(3, 8, "De otro organizador", "DRAFT");
        insertSection(101, 1, "100000.00");
        insertSection(102, 1, "30000.00");
        insertSection(201, 2, "50000.00");
        insertSection(301, 3, "10000.00");

        List<Seat> rows = new ArrayList<>(seats(1, 101, 4));
        rows.add(courtesy(101, 1));
        rows.addAll(seats(1, 102, 10));
        rows.addAll(seats(1, 201, 5));
        rows.addAll(seats(1, 301, 5));
        seatRepository.insertAll(rows);

        List<EventSeatSummary> summaries = seatRepository.summarizeSeatsByOrganizer(7L);

        assertThat(summaries).singleElement().satisfies(s -> {
            assertThat(s.eventId()).isEqualTo(1L);
            assertThat(s.eventName()).isEqualTo("Concierto");
            // 4 × 100.000 + 10 × 30.000; la cortesía no suma
            assertThat(s.potentialRevenue()).isEqualByComparingTo("700000");
            assertThat(s.courtesySeats()).isEqualTo(1L);
            assertThat(s.totalSeats()).isEqualTo(15L);
        });
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
