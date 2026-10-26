package com.capysoft.tuevento.modules.ticket.application.usecase;

import com.capysoft.tuevento.modules.seat.domain.model.Seat;
import com.capysoft.tuevento.modules.seat.domain.model.SeatStatus;
import com.capysoft.tuevento.modules.seat.domain.repository.SeatRepository;
import com.capysoft.tuevento.modules.section.domain.model.EventSection;
import com.capysoft.tuevento.modules.section.domain.repository.EventSectionRepository;
import com.capysoft.tuevento.modules.ticket.application.dto.request.CreateOrderRequest;
import com.capysoft.tuevento.modules.ticket.domain.repository.OrderRepository;
import com.capysoft.tuevento.modules.ticket.domain.repository.SeatTicketRepository;
import com.capysoft.tuevento.modules.ticket.domain.repository.TicketLogRepository;
import com.capysoft.tuevento.modules.ticket.domain.repository.TicketRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Validaciones antes de crear una orden: cada silla debe seguir RESERVADA por el comprador
 * y pertenecer al evento de la orden.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CreateOrderWithTicketsUseCaseImpl — validación de sillas")
class CreateOrderWithTicketsValidationTest {

    private static final long USER_ID = 7L;
    private static final long EVENT_ID = 5L;
    private static final int SEAT_ID = 10;
    private static final int SECTION_ID = 70;

    @Mock OrderRepository orderRepository;
    @Mock TicketRepository ticketRepository;
    @Mock SeatTicketRepository seatTicketRepository;
    @Mock TicketLogRepository ticketLogRepository;
    @Mock SeatRepository seatRepository;
    @Mock EventSectionRepository eventSectionRepository;

    @InjectMocks CreateOrderWithTicketsUseCaseImpl useCase;

    private void seatIs(SeatStatus status) {
        when(seatRepository.findByIdForUpdate(SEAT_ID)).thenReturn(Optional.of(Seat.builder()
                .seatId(SEAT_ID).eventSectionId(SECTION_ID).status(status)
                .reservedBy((int) USER_ID).reservedUntil(LocalDateTime.now().plusMinutes(5)).build()));
    }

    private static CreateOrderRequest request() {
        return new CreateOrderRequest(EVENT_ID, List.of(SEAT_ID));
    }

    @ParameterizedTest(name = "silla {0}")
    @EnumSource(value = SeatStatus.class, names = {"AVAILABLE", "SOLD", "COURTESY"})
    @DisplayName("una silla que ya no está RESERVADA no entra en la orden, aunque conserve reservedBy")
    void seatMustStillBeReserved(SeatStatus status) {
        seatIs(status);

        assertThatThrownBy(() -> useCase.execute(request(), USER_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("is not reserved");
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("no se pueden mezclar sillas de otro evento en la orden")
    void seatMustBelongToTheOrderEvent() {
        seatIs(SeatStatus.RESERVED);
        when(eventSectionRepository.findById(SECTION_ID)).thenReturn(Optional.of(
                EventSection.builder().eventSectionId(SECTION_ID).eventId(99).price(BigDecimal.TEN).build()));

        assertThatThrownBy(() -> useCase.execute(request(), USER_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("does not belong to event");
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("una reserva de otro usuario o vencida se rechaza")
    void reservationMustBeOwnAndActive() {
        when(seatRepository.findByIdForUpdate(SEAT_ID)).thenReturn(Optional.of(Seat.builder()
                .seatId(SEAT_ID).eventSectionId(SECTION_ID).status(SeatStatus.RESERVED)
                .reservedBy(8).reservedUntil(LocalDateTime.now().plusMinutes(5)).build()));
        assertThatThrownBy(() -> useCase.execute(request(), USER_ID)).hasMessageContaining("not reserved by this user");

        when(seatRepository.findByIdForUpdate(SEAT_ID)).thenReturn(Optional.of(Seat.builder()
                .seatId(SEAT_ID).eventSectionId(SECTION_ID).status(SeatStatus.RESERVED)
                .reservedBy((int) USER_ID).reservedUntil(LocalDateTime.now().minusMinutes(1)).build()));
        assertThatThrownBy(() -> useCase.execute(request(), USER_ID)).hasMessageContaining("expired");
    }

    @Test
    @DisplayName("las sillas se bloquean en orden ascendente y una repetida se bloquea una sola vez")
    void seatsAreLockedInAscendingOrderWithoutRepeats() {
        // La 10 (la menor) falla: si se bloqueara primero la 12, este test lo detectaría
        seatIs(SeatStatus.AVAILABLE);
        CreateOrderRequest request = new CreateOrderRequest(EVENT_ID, List.of(12, SEAT_ID, 12));

        assertThatThrownBy(() -> useCase.execute(request, USER_ID)).isInstanceOf(IllegalStateException.class);
        verify(seatRepository).findByIdForUpdate(SEAT_ID);
        verify(seatRepository, never()).findByIdForUpdate(12);
        verify(seatRepository, never()).findById(any());
    }
}
