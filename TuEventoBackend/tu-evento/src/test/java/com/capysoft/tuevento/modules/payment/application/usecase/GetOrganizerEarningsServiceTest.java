package com.capysoft.tuevento.modules.payment.application.usecase;

import com.capysoft.tuevento.modules.payment.application.dto.response.OrganizerEarningsByEventResponse;
import com.capysoft.tuevento.modules.payment.application.dto.response.OrganizerEarningsResponse;
import com.capysoft.tuevento.modules.payment.domain.repository.PaymentRepository;
import com.capysoft.tuevento.modules.payment.application.port.out.OrganizerSeatsPort;
import com.capysoft.tuevento.modules.payment.application.port.out.OrganizerSeatsPort.EventSeats;
import com.capysoft.tuevento.modules.ticket.application.port.out.EventLookupPort;
import com.capysoft.tuevento.modules.ticket.application.port.out.EventLookupPort.EventRef;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Panel de ganancias: lo cobrado viene de los pagos aprobados y lo potencial de las sillas
 * que no son de cortesía. Las cortesías nunca restan de lo cobrado.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetOrganizerEarningsService — cobrado y ganancia potencial")
class GetOrganizerEarningsServiceTest {

    private static final long ORGANIZER_ID = 7L;

    @Mock PaymentRepository paymentRepository;
    @Mock OrganizerSeatsPort organizerSeatsPort;
    @Mock EventLookupPort eventLookupPort;

    private GetOrganizerEarningsService service;

    @BeforeEach
    void setUp() {
        service = new GetOrganizerEarningsService(paymentRepository, organizerSeatsPort, eventLookupPort);
    }

    private static OrganizerEarningsByEventResponse paid(long eventId, int tickets, String revenue) {
        return new OrganizerEarningsByEventResponse(eventId, tickets, new BigDecimal(revenue), "COP");
    }

    private static EventRef event(long eventId, String name) {
        return new EventRef(eventId, ORGANIZER_ID, name, "PUBLISHED");
    }

    private static EventSeats seats(long eventId, String name, String potential, long courtesy, long total) {
        return new EventSeats(eventId, name, new BigDecimal(potential), courtesy, total);
    }

    @Test
    @DisplayName("une lo cobrado y lo potencial por evento, incluidos los eventos aún sin ventas")
    void mergesRevenueAndPotentialPerEvent() {
        when(eventLookupPort.findByOwner(ORGANIZER_ID)).thenReturn(List.of(event(1, "Concierto"), event(2, "Teatro")));
        when(paymentRepository.findEarningsByEvents(Set.of(1L, 2L))).thenReturn(List.of(paid(1, 4, "400000")));
        when(organizerSeatsPort.summarizeByOrganizer(ORGANIZER_ID)).thenReturn(List.of(
                seats(2, "Teatro", "900000", 0, 30),
                seats(1, "Concierto", "1000000", 3, 13)));

        OrganizerEarningsResponse response = service.execute(ORGANIZER_ID);

        assertThat(response.getTotalEarnings()).isEqualByComparingTo("400000");
        assertThat(response.getTotalTicketsSold()).isEqualTo(4);
        assertThat(response.getTotalPotentialEarnings()).isEqualByComparingTo("1900000");
        assertThat(response.getTotalCourtesySeats()).isEqualTo(3);
        assertThat(response.getCurrency()).isEqualTo("COP");

        // Primero los que más han cobrado; después, por ganancia potencial
        assertThat(response.getEarningsByEvent()).extracting(OrganizerEarningsByEventResponse::getEventId)
                .containsExactly(1L, 2L);
        OrganizerEarningsByEventResponse concert = response.getEarningsByEvent().get(0);
        assertThat(concert.getEventName()).isEqualTo("Concierto");
        assertThat(concert.getTotalRevenue()).isEqualByComparingTo("400000");
        assertThat(concert.getPotentialRevenue()).isEqualByComparingTo("1000000");
        assertThat(concert.getCourtesySeats()).isEqualTo(3);
        assertThat(concert.getTotalSeats()).isEqualTo(13);

        OrganizerEarningsByEventResponse theater = response.getEarningsByEvent().get(1);
        assertThat(theater.getEventName()).isEqualTo("Teatro");
        assertThat(theater.getTicketsSold()).isZero();
        assertThat(theater.getTotalRevenue()).isEqualByComparingTo("0");
        assertThat(theater.getCurrency()).isEqualTo("COP");
    }

    @Test
    @DisplayName("un evento con ventas pero sin sillas (p. ej. cancelado) conserva lo cobrado y potencial 0")
    void keepsRevenueOfEventsWithoutSeats() {
        when(eventLookupPort.findByOwner(ORGANIZER_ID)).thenReturn(List.of(event(5, "Cancelado")));
        when(paymentRepository.findEarningsByEvents(Set.of(5L))).thenReturn(List.of(paid(5, 2, "120000")));
        when(organizerSeatsPort.summarizeByOrganizer(ORGANIZER_ID)).thenReturn(List.of());

        OrganizerEarningsByEventResponse event = service.execute(ORGANIZER_ID).getEarningsByEvent().get(0);

        assertThat(event.getTotalRevenue()).isEqualByComparingTo("120000");
        assertThat(event.getPotentialRevenue()).isEqualByComparingTo("0");
        assertThat(event.getTotalSeats()).isZero();
    }

    @Test
    @DisplayName("sin eventos devuelve todo en cero y moneda COP")
    void emptyOrganizer() {
        when(eventLookupPort.findByOwner(ORGANIZER_ID)).thenReturn(List.of());
        when(organizerSeatsPort.summarizeByOrganizer(ORGANIZER_ID)).thenReturn(List.of());

        OrganizerEarningsResponse response = service.execute(ORGANIZER_ID);

        assertThat(response.getEarningsByEvent()).isEmpty();
        assertThat(response.getTotalEarnings()).isEqualByComparingTo("0");
        assertThat(response.getTotalPotentialEarnings()).isEqualByComparingTo("0");
        assertThat(response.getCurrency()).isEqualTo("COP");
        verifyNoInteractions(paymentRepository);
    }
}
