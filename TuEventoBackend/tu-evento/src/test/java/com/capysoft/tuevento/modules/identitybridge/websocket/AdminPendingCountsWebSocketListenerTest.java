package com.capysoft.tuevento.modules.identitybridge.websocket;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.capysoft.tuevento.modules.event.domain.event.EventStatusChangedEvent;
import com.capysoft.tuevento.modules.identitybridge.client.IdentityServiceClient;
import com.capysoft.tuevento.modules.identitybridge.port.AdminPanelSourcesPort;
import com.capysoft.tuevento.modules.identitybridge.websocket.AdminPendingCountsWebSocketListener.PendingCounts;
import com.capysoft.tuevento.shared.domain.exception.ServiceUnavailableException;
import com.capysoft.tuevento.shared.infrastructure.websocket.AdminPendingCountsChangedEvent;

@DisplayName("AdminPendingCountsWebSocketListener")
class AdminPendingCountsWebSocketListenerTest {

    private final SimpMessagingTemplate messaging = mock(SimpMessagingTemplate.class);
    private final IdentityServiceClient identity = mock(IdentityServiceClient.class);
    private final AdminPanelSourcesPort sources = mock(AdminPanelSourcesPort.class);
    private final AdminPendingCountsWebSocketListener listener =
            new AdminPendingCountsWebSocketListener(messaging, identity, sources);

    @Test
    @DisplayName("a change in identity-service pushes the organizer requests and events in review")
    void broadcastsCombinedCounts() {
        when(identity.pendingOrganizerRequests()).thenReturn(2L);
        when(sources.eventsPendingReview()).thenReturn(1L);

        listener.onChangedInIdentity(new AdminPendingCountsChangedEvent());

        verify(messaging).convertAndSend(AdminPendingCountsWebSocketListener.TOPIC, new PendingCounts(2, 1));
    }

    @Test
    @DisplayName("only event transitions into or out of PENDING_REVIEW push the counts")
    void eventTransitions() {
        listener.onEventStatusChanged(EventStatusChangedEvent.builder().oldStatus("DRAFT").newStatus("PENDING_REVIEW").build());
        listener.onEventStatusChanged(EventStatusChangedEvent.builder().oldStatus("PENDING_REVIEW").newStatus("PUBLISHED").build());
        listener.onEventStatusChanged(EventStatusChangedEvent.builder().oldStatus("PUBLISHED").newStatus("CANCELLED").build());

        verify(messaging, times(2)).convertAndSend(eq(AdminPendingCountsWebSocketListener.TOPIC), any(Object.class));
    }

    @Test
    @DisplayName("when identity-service is down nothing is pushed (no false zero) and the flow goes on")
    void identityDownSendsNothing() {
        when(identity.pendingOrganizerRequests()).thenThrow(new ServiceUnavailableException("IDENTITY_UNAVAILABLE", "down"));

        assertThatCode(() -> listener.onChangedInIdentity(new AdminPendingCountsChangedEvent())).doesNotThrowAnyException();
        verify(messaging, never()).convertAndSend(eq(AdminPendingCountsWebSocketListener.TOPIC), any(Object.class));
    }
}
