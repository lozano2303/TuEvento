package com.capysoft.tuevento.modules.ticket.infrastructure.client;

import com.capysoft.tuevento.modules.seat.application.dto.request.UpdateSeatStatusRequest;
import com.capysoft.tuevento.modules.seat.application.port.in.SeatUseCase;
import com.capysoft.tuevento.modules.seat.domain.model.Seat;
import com.capysoft.tuevento.modules.seat.domain.model.SeatStatus;
import com.capysoft.tuevento.modules.seat.domain.repository.SeatRepository;
import com.capysoft.tuevento.modules.section.domain.exception.EventSectionNotFoundException;
import com.capysoft.tuevento.modules.section.domain.model.EventSection;
import com.capysoft.tuevento.modules.section.domain.repository.EventSectionRepository;
import com.capysoft.tuevento.modules.ticket.application.port.out.CatalogSeatsPort.HeldSeat;
import com.capysoft.tuevento.modules.ticket.application.port.out.CatalogSeatsPort.HeldSeats;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Bloquear sillas para una orden: cada silla debe seguir RESERVADA por el comprador, con la
 * reserva vigente, y pertenecer al evento de la orden. Vender y liberar delegan en seat.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CatalogSeatsAdapter — sillas de una orden")
class CatalogSeatsAdapterTest {

    private static final long ORDER_ID = 40L;
    private static final long USER_ID = 7L;
    private static final long EVENT_ID = 5L;
    private static final int SEAT_ID = 10;
    private static final int SECTION_ID = 70;

    @Mock SeatRepository seatRepository;
    @Mock EventSectionRepository eventSectionRepository;
    @Mock SeatUseCase seatUseCase;

    @InjectMocks CatalogSeatsAdapter adapter;

    private static Seat seat(int seatId, int sectionId, SeatStatus status) {
        return Seat.builder().seatId(seatId).eventSectionId(sectionId).status(status)
                .reservedBy((int) USER_ID).reservedUntil(LocalDateTime.now().plusMinutes(5)).build();
    }

    private void seatIs(SeatStatus status) {
        when(seatRepository.findByIdForUpdate(SEAT_ID)).thenReturn(Optional.of(seat(SEAT_ID, SECTION_ID, status)));
    }

    private void section(int sectionId, int eventId, String price) {
        when(eventSectionRepository.findById(sectionId)).thenReturn(Optional.of(
                EventSection.builder().eventSectionId(sectionId).eventId(eventId).price(new BigDecimal(price)).build()));
    }

    private HeldSeats hold(List<Integer> seatIds) {
        return adapter.holdForOrder(ORDER_ID, USER_ID, EVENT_ID, seatIds);
    }

    @ParameterizedTest(name = "silla {0}")
    @EnumSource(value = SeatStatus.class, names = {"AVAILABLE", "SOLD", "COURTESY"})
    @DisplayName("una silla que ya no está RESERVADA no se bloquea, aunque conserve reservedBy")
    void seatMustStillBeReserved(SeatStatus status) {
        seatIs(status);

        assertThatThrownBy(() -> hold(List.of(SEAT_ID)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("is not reserved");
    }

    @Test
    @DisplayName("una silla inexistente se rechaza")
    void missingSeat() {
        when(seatRepository.findByIdForUpdate(SEAT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> hold(List.of(SEAT_ID)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Seat not found");
    }

    @Test
    @DisplayName("no se pueden mezclar sillas de otro evento en la orden")
    void seatMustBelongToTheOrderEvent() {
        seatIs(SeatStatus.RESERVED);
        section(SECTION_ID, 99, "10");

        assertThatThrownBy(() -> hold(List.of(SEAT_ID)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("does not belong to event");
    }

    @Test
    @DisplayName("una sección inexistente se rechaza")
    void missingSection() {
        seatIs(SeatStatus.RESERVED);
        when(eventSectionRepository.findById(SECTION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> hold(List.of(SEAT_ID))).isInstanceOf(EventSectionNotFoundException.class);
    }

    @Test
    @DisplayName("una reserva de otro usuario, sin dueño o vencida se rechaza")
    void reservationMustBeOwnAndActive() {
        when(seatRepository.findByIdForUpdate(SEAT_ID)).thenReturn(Optional.of(Seat.builder()
                .seatId(SEAT_ID).eventSectionId(SECTION_ID).status(SeatStatus.RESERVED)
                .reservedBy(8).reservedUntil(LocalDateTime.now().plusMinutes(5)).build()));
        assertThatThrownBy(() -> hold(List.of(SEAT_ID))).hasMessageContaining("not reserved by this user");

        when(seatRepository.findByIdForUpdate(SEAT_ID)).thenReturn(Optional.of(Seat.builder()
                .seatId(SEAT_ID).eventSectionId(SECTION_ID).status(SeatStatus.RESERVED)
                .reservedUntil(LocalDateTime.now().plusMinutes(5)).build()));
        assertThatThrownBy(() -> hold(List.of(SEAT_ID))).hasMessageContaining("not reserved by this user");

        when(seatRepository.findByIdForUpdate(SEAT_ID)).thenReturn(Optional.of(Seat.builder()
                .seatId(SEAT_ID).eventSectionId(SECTION_ID).status(SeatStatus.RESERVED)
                .reservedBy((int) USER_ID).reservedUntil(LocalDateTime.now().minusMinutes(1)).build()));
        assertThatThrownBy(() -> hold(List.of(SEAT_ID))).hasMessageContaining("expired");

        when(seatRepository.findByIdForUpdate(SEAT_ID)).thenReturn(Optional.of(Seat.builder()
                .seatId(SEAT_ID).eventSectionId(SECTION_ID).status(SeatStatus.RESERVED)
                .reservedBy((int) USER_ID).build()));
        assertThatThrownBy(() -> hold(List.of(SEAT_ID))).hasMessageContaining("expired");
    }

    @Test
    @DisplayName("devuelve cada silla con el precio de su sección, en el orden pedido; una sección se lee una vez")
    void returnsSeatPricesInRequestOrder() {
        when(seatRepository.findByIdForUpdate(10)).thenReturn(Optional.of(seat(10, 70, SeatStatus.RESERVED)));
        when(seatRepository.findByIdForUpdate(11)).thenReturn(Optional.of(seat(11, 71, SeatStatus.RESERVED)));
        when(seatRepository.findByIdForUpdate(12)).thenReturn(Optional.of(seat(12, 70, SeatStatus.RESERVED)));
        section(70, (int) EVENT_ID, "100");
        section(71, (int) EVENT_ID, "250");

        HeldSeats held = hold(List.of(10, 11, 12));

        assertThat(held.currency()).isEqualTo("COP");
        assertThat(held.seats()).containsExactly(
                new HeldSeat(10, 70, new BigDecimal("100")),
                new HeldSeat(11, 71, new BigDecimal("250")),
                new HeldSeat(12, 70, new BigDecimal("100")));
        assertThat(held.total()).isEqualByComparingTo("450");
        verify(eventSectionRepository, times(1)).findById(70);
    }

    @Test
    @DisplayName("si una silla falla, no se lee ninguna sección")
    void stopsBeforeSectionsWhenASeatFails() {
        seatIs(SeatStatus.AVAILABLE);

        assertThatThrownBy(() -> hold(List.of(SEAT_ID, 12))).isInstanceOf(IllegalStateException.class);
        verify(seatRepository, never()).findByIdForUpdate(12);
        verify(eventSectionRepository, never()).findById(any());
    }

    @Test
    @DisplayName("vender → cada silla a nombre del comprador")
    void sellsEverySeat() {
        adapter.sell(ORDER_ID, USER_ID, List.of(10, 11), "Payment confirmed: gw-1");

        verify(seatUseCase).sellSeat(10, USER_ID, "Payment confirmed: gw-1");
        verify(seatUseCase).sellSeat(11, USER_ID, "Payment confirmed: gw-1");
    }

    @Test
    @DisplayName("liberar → cada silla vuelve a AVAILABLE con el motivo y quién la liberó")
    void releasesEverySeat() {
        adapter.release(ORDER_ID, List.of(10, 11), 7, "Order cancelled");

        ArgumentCaptor<UpdateSeatStatusRequest> request = ArgumentCaptor.forClass(UpdateSeatStatusRequest.class);
        verify(seatUseCase).updateSeatStatus(eq(10), request.capture(), eq(7));
        verify(seatUseCase).updateSeatStatus(eq(11), any(), eq(7));
        assertThat(request.getValue().getNewStatus()).isEqualTo(SeatStatus.AVAILABLE);
        assertThat(request.getValue().getReason()).isEqualTo("Order cancelled");
    }
}
