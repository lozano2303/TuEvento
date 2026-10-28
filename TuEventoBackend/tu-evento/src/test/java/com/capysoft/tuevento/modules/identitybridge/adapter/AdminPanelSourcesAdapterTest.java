package com.capysoft.tuevento.modules.identitybridge.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.capysoft.tuevento.modules.event.application.port.in.GetEventAdminStatsPort;
import com.capysoft.tuevento.modules.identitybridge.port.AdminPanelSourcesPort;
import com.capysoft.tuevento.modules.payment.application.port.in.GetCommerceAdminStatsPort;

@DisplayName("AdminPanelSourcesAdapter")
class AdminPanelSourcesAdapterTest {

    private static final LocalDateTime AT = LocalDateTime.of(2026, 10, 27, 12, 0);

    private final GetEventAdminStatsPort events = mock(GetEventAdminStatsPort.class);
    private final GetCommerceAdminStatsPort commerce = mock(GetCommerceAdminStatsPort.class);
    private final AdminPanelSourcesAdapter adapter = new AdminPanelSourcesAdapter(events, commerce);

    @Test
    @DisplayName("maps the catalog figures and recent events for the admin panel")
    void catalog() {
        when(events.getStats()).thenReturn(new GetEventAdminStatsPort.EventStats(10, 5, 2, 1, 1, 0, 1));
        when(events.countPendingReview()).thenReturn(2L);
        when(events.findRecentlyUpdated(3)).thenReturn(List.of(
                new GetEventAdminStatsPort.RecentEvent("Concert", "PUBLISHED", 7L, AT)));

        assertThat(adapter.catalogStats()).isEqualTo(new AdminPanelSourcesPort.CatalogStats(10, 5, 2, 1, 1, 0, 1));
        assertThat(adapter.eventsPendingReview()).isEqualTo(2L);
        assertThat(adapter.recentlyUpdatedEvents(3))
                .containsExactly(new AdminPanelSourcesPort.RecentEvent("Concert", "PUBLISHED", 7L, AT));
    }

    @Test
    @DisplayName("maps the commerce figures and recent payments for the admin panel")
    void commerce() {
        when(commerce.getStats()).thenReturn(new GetCommerceAdminStatsPort.CommerceStats(
                30, BigDecimal.TEN, 12, 2, BigDecimal.ONE, 1));
        when(commerce.findRecentPayments(3)).thenReturn(List.of(
                new GetCommerceAdminStatsPort.RecentPayment(true, BigDecimal.TEN, "COP", 5L, AT)));

        assertThat(adapter.commerceStats())
                .isEqualTo(new AdminPanelSourcesPort.CommerceStats(30, BigDecimal.TEN, 12, 2, BigDecimal.ONE, 1));
        assertThat(adapter.recentPayments(3))
                .containsExactly(new AdminPanelSourcesPort.RecentPayment(true, BigDecimal.TEN, "COP", 5L, AT));
    }
}
