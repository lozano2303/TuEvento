package com.capysoft.commerce.payment.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.capysoft.commerce.infrastructure.client.CatalogServiceClient;
import com.capysoft.commerce.payment.application.port.out.OrganizerSeatsPort.EventSeats;

@DisplayName("CatalogOrganizerSeatsAdapter")
class CatalogOrganizerSeatsAdapterTest {

    @Test
    @DisplayName("copia el resumen de sillas que da Catálogo")
    void maps() {
        CatalogServiceClient catalog = mock(CatalogServiceClient.class);
        when(catalog.seatSummary(7L)).thenReturn(List.of(
                new CatalogServiceClient.EventSeatSummary(3L, "Concierto", BigDecimal.TEN, 1L, 2L)));

        assertThat(new CatalogOrganizerSeatsAdapter(catalog).summarizeByOrganizer(7L))
                .containsExactly(new EventSeats(3L, "Concierto", BigDecimal.TEN, 1L, 2L));
    }
}
