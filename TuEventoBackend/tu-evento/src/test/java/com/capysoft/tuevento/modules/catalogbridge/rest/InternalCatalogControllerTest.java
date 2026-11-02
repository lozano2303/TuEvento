package com.capysoft.tuevento.modules.catalogbridge.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.capysoft.tuevento.modules.catalogbridge.application.HoldSeatsForOrderService;
import com.capysoft.tuevento.modules.catalogbridge.application.HoldSeatsForOrderService.HeldSeats;
import com.capysoft.tuevento.modules.catalogbridge.application.HoldSeatsForOrderService.HoldRejectedException;
import com.capysoft.tuevento.modules.catalogbridge.rest.InternalCatalogController.HoldRequest;
import com.capysoft.tuevento.modules.seat.domain.model.EventSeatSummary;
import com.capysoft.tuevento.modules.seat.domain.repository.SeatRepository;

@DisplayName("InternalCatalogController")
class InternalCatalogControllerTest {

    private final HoldSeatsForOrderService hold = mock(HoldSeatsForOrderService.class);
    private final SeatRepository seats = mock(SeatRepository.class);
    private final InternalCatalogController controller = new InternalCatalogController(hold, seats);

    @Test
    @DisplayName("hold devuelve las sillas bloqueadas")
    void holds() {
        HeldSeats held = new HeldSeats("COP", List.of());
        when(hold.hold(5L, 3L, List.of(1))).thenReturn(held);

        assertThat(controller.hold(new HoldRequest(9L, 5L, 3L, List.of(1))).getBody().getData()).isEqualTo(held);
    }

    @Test
    @DisplayName("un hold rechazado responde 409 con su código")
    void rejected() {
        var response = controller.onHoldRejected(new HoldRejectedException("SEAT_NOT_RESERVED", "no"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getCode()).isEqualTo("SEAT_NOT_RESERVED");
    }

    @Test
    @DisplayName("seat-summary expone el resumen del organizador")
    void summary() {
        List<EventSeatSummary> rows = List.of(new EventSeatSummary(3L, "Concierto", BigDecimal.TEN, 1L, 2L));
        when(seats.summarizeSeatsByOrganizer(7L)).thenReturn(rows);

        assertThat(controller.seatSummary(7L).getBody().getData()).isEqualTo(rows);
    }
}
