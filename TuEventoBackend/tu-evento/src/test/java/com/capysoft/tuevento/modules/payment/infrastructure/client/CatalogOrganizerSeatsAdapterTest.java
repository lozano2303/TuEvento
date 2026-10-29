package com.capysoft.tuevento.modules.payment.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.capysoft.tuevento.modules.payment.application.port.out.OrganizerSeatsPort.EventSeats;
import com.capysoft.tuevento.modules.seat.domain.model.EventSeatSummary;
import com.capysoft.tuevento.modules.seat.domain.repository.SeatRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("CatalogOrganizerSeatsAdapter — sillas del organizador desde Catálogo")
class CatalogOrganizerSeatsAdapterTest {

    @Mock SeatRepository seatRepository;
    @InjectMocks CatalogOrganizerSeatsAdapter adapter;

    @Test
    @DisplayName("copia cada resumen de seat al modelo propio de payment")
    void mapsSeatSummaries() {
        when(seatRepository.summarizeSeatsByOrganizer(7L)).thenReturn(List.of(
                new EventSeatSummary(1L, "Concierto", new BigDecimal("1000000"), 3L, 13L)));

        assertThat(adapter.summarizeByOrganizer(7L)).containsExactly(
                new EventSeats(1L, "Concierto", new BigDecimal("1000000"), 3L, 13L));
    }

    @Test
    @DisplayName("sin eventos con sillas devuelve una lista vacía")
    void emptyWhenNoSeats() {
        when(seatRepository.summarizeSeatsByOrganizer(7L)).thenReturn(List.of());

        assertThat(adapter.summarizeByOrganizer(7L)).isEmpty();
    }
}
