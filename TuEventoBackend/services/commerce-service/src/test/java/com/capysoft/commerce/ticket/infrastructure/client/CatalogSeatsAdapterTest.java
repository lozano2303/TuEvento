package com.capysoft.commerce.ticket.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.capysoft.commerce.infrastructure.client.CatalogServiceClient;
import com.capysoft.commerce.shared.outbox.OutboxEventPublisher;
import com.capysoft.commerce.ticket.application.port.out.CatalogSeatsPort.HeldSeats;
import com.capysoft.commerce.ticket.application.port.out.CatalogSeatsPort.ReleaseReason;

@DisplayName("CatalogSeatsAdapter — la saga vista desde Comercio")
class CatalogSeatsAdapterTest {

    private final CatalogServiceClient catalog = mock(CatalogServiceClient.class);
    private final OutboxEventPublisher outbox = mock(OutboxEventPublisher.class);
    private final CatalogSeatsAdapter adapter = new CatalogSeatsAdapter(catalog, outbox);

    @Test
    @DisplayName("el bloqueo es síncrono y devuelve el total de las sillas")
    void holdIsSynchronous() {
        when(catalog.hold(9L, 5L, 3L, List.of(1, 2))).thenReturn(new CatalogServiceClient.HeldSeats("COP", List.of(
                new CatalogServiceClient.HeldSeat(1, 7, new BigDecimal("1000")),
                new CatalogServiceClient.HeldSeat(2, 7, new BigDecimal("2000")))));

        HeldSeats held = adapter.holdForOrder(9L, 5L, 3L, List.of(1, 2));

        assertThat(held.currency()).isEqualTo("COP");
        assertThat(held.total()).isEqualByComparingTo("3000");
    }

    @Test
    @DisplayName("vender publica OrderPaid con la clave de la orden")
    void sellPublishes() {
        adapter.sell(9L, 5L, 3L, List.of(1), "tx");

        verify(outbox).publish("order", "9", "OrderPaid", new CatalogSeatsAdapter.OrderPaid(9L, 5L, 3L, List.of(1), "tx"));
    }

    @Test
    @DisplayName("liberar publica OrderReleased con el comprador y las sillas")
    void releasePublishes() {
        adapter.release(9L, 5L, List.of(1), 5, ReleaseReason.CANCELLED, "Order cancelled");

        verify(outbox).publish("order", "9", "OrderReleased", new CatalogSeatsAdapter.OrderReleased(
                9L, 5L, List.of(1), ReleaseReason.CANCELLED, "Order cancelled", 5));
    }
}
