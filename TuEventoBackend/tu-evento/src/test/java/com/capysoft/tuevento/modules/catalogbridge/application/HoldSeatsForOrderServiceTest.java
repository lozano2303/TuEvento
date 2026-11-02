package com.capysoft.tuevento.modules.catalogbridge.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import com.capysoft.tuevento.modules.catalogbridge.application.HoldSeatsForOrderService.HeldSeat;
import com.capysoft.tuevento.modules.catalogbridge.application.HoldSeatsForOrderService.HoldRejectedException;
import com.capysoft.tuevento.modules.seat.domain.model.Seat;
import com.capysoft.tuevento.modules.seat.domain.model.SeatStatus;
import com.capysoft.tuevento.modules.seat.domain.repository.SeatRepository;
import com.capysoft.tuevento.modules.section.domain.model.EventSection;
import com.capysoft.tuevento.modules.section.domain.repository.EventSectionRepository;

@DisplayName("HoldSeatsForOrderService — paso 1 de la saga")
class HoldSeatsForOrderServiceTest {

    private static final long USER = 5L;
    private static final long EVENT = 3L;

    private final SeatRepository seats = mock(SeatRepository.class);
    private final EventSectionRepository sections = mock(EventSectionRepository.class);
    private final HoldSeatsForOrderService service = new HoldSeatsForOrderService(seats, sections);

    private void seat(int id, SeatStatus status, Integer reservedBy, LocalDateTime until) {
        when(seats.findByIdForUpdate(id)).thenReturn(Optional.of(Seat.builder().seatId(id).eventSectionId(9)
                .status(status).reservedBy(reservedBy).reservedUntil(until).build()));
    }

    private void section(int eventId) {
        when(sections.findById(9)).thenReturn(Optional.of(EventSection.builder().eventSectionId(9).eventId(eventId)
                .price(new BigDecimal("1000")).build()));
    }

    private static LocalDateTime future() {
        return LocalDateTime.now().plusMinutes(5);
    }

    private void assertRejected(List<Integer> ids, String code) {
        assertThatThrownBy(() -> service.hold(USER, EVENT, ids))
                .isInstanceOf(HoldRejectedException.class)
                .satisfies(e -> assertThat(((HoldRejectedException) e).getCode()).isEqualTo(code));
    }

    @Test
    @DisplayName("bloquea en orden de seatId, sin repetidas, y devuelve el precio de cada silla")
    void holdsInOrder() {
        seat(1, SeatStatus.RESERVED, 5, future());
        seat(2, SeatStatus.RESERVED, 5, future());
        section(3);

        var held = service.hold(USER, EVENT, List.of(2, 1, 2));

        assertThat(held.currency()).isEqualTo("COP");
        assertThat(held.seats()).containsExactly(new HeldSeat(1, 9, new BigDecimal("1000")),
                new HeldSeat(2, 9, new BigDecimal("1000")));
        InOrder order = inOrder(seats);
        order.verify(seats).findByIdForUpdate(1);
        order.verify(seats).findByIdForUpdate(2);
    }

    @Test
    @DisplayName("rechaza una silla inexistente, no reservada, de otro usuario, sin dueño o sin plazo")
    void rejectsNotReserved() {
        when(seats.findByIdForUpdate(1)).thenReturn(Optional.empty());
        assertRejected(List.of(1), "SEAT_NOT_FOUND");

        seat(1, SeatStatus.SOLD, 5, future());
        assertRejected(List.of(1), "SEAT_NOT_RESERVED");

        seat(1, SeatStatus.RESERVED, 6, future());
        assertRejected(List.of(1), "SEAT_NOT_RESERVED");

        seat(1, SeatStatus.RESERVED, null, future());
        assertRejected(List.of(1), "SEAT_NOT_RESERVED");
    }

    @Test
    @DisplayName("rechaza una reserva vencida o sin fecha")
    void rejectsExpired() {
        seat(1, SeatStatus.RESERVED, 5, LocalDateTime.now().minusSeconds(1));
        assertRejected(List.of(1), "SEAT_RESERVATION_EXPIRED");

        seat(1, SeatStatus.RESERVED, 5, null);
        assertRejected(List.of(1), "SEAT_RESERVATION_EXPIRED");
    }

    @Test
    @DisplayName("rechaza una silla de otro evento o de una sección que no existe")
    void rejectsOtherEvent() {
        seat(1, SeatStatus.RESERVED, 5, future());
        section(4);
        assertRejected(List.of(1), "SEAT_NOT_IN_EVENT");

        when(sections.findById(9)).thenReturn(Optional.empty());
        assertRejected(List.of(1), "SEAT_NOT_IN_EVENT");
    }
}
