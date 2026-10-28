package com.capysoft.tuevento.modules.security.interfaces.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.capysoft.tuevento.modules.security.application.port.out.AdminPanelSourcesPort;

@DisplayName("InternalAdminPanelController")
class InternalAdminPanelControllerTest {

    private final AdminPanelSourcesPort sources = mock(AdminPanelSourcesPort.class);
    private final InternalAdminPanelController controller = new InternalAdminPanelController(sources);

    @Test
    @DisplayName("serves the catalog and commerce figures straight from the panel sources")
    void servesStats() {
        when(sources.eventsPendingReview()).thenReturn(4L);

        assertThat(controller.eventsPendingReview().getBody().getData()).isEqualTo(4L);
        assertThat(controller.catalogStats().getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(controller.commerceStats().getStatusCode().is2xxSuccessful()).isTrue();
        verify(sources).catalogStats();
        verify(sources).commerceStats();
    }

    @Test
    @DisplayName("keeps the activity limit between 1 and 100, like the panel")
    void clampsActivityLimit() {
        when(sources.recentlyUpdatedEvents(InternalAdminPanelController.MAX_LIMIT)).thenReturn(List.of());

        assertThat(controller.recentEvents(500).getBody().getData()).isEmpty();
        controller.recentPayments(0);
        controller.recentPayments(20);

        verify(sources).recentlyUpdatedEvents(InternalAdminPanelController.MAX_LIMIT);
        verify(sources).recentPayments(1);
        verify(sources).recentPayments(20);
    }
}
