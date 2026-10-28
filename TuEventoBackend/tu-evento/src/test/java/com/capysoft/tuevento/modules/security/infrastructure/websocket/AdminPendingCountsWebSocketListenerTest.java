package com.capysoft.tuevento.modules.security.infrastructure.websocket;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.capysoft.tuevento.modules.event.domain.event.EventStatusChangedEvent;
import com.capysoft.tuevento.modules.security.application.dto.response.AdminPendingCountsResponse;
import com.capysoft.tuevento.modules.security.application.port.in.GetAdminPendingCountsPort;
import com.capysoft.tuevento.shared.infrastructure.websocket.AdminPendingCountsChangedEvent;

@DisplayName("AdminPendingCountsWebSocketListener")
class AdminPendingCountsWebSocketListenerTest {

    private final SimpMessagingTemplate messaging = mock(SimpMessagingTemplate.class);
    private final GetAdminPendingCountsPort counts = mock(GetAdminPendingCountsPort.class);
    private final AdminPendingCountsWebSocketListener listener = new AdminPendingCountsWebSocketListener(messaging, counts);

    @BeforeEach
    void stubCounts() {
        when(counts.getCounts()).thenReturn(AdminPendingCountsResponse.builder().organizerRequests(2).eventsInReview(1).build());
    }

    @Test
    @DisplayName("a change announced by identity-service pushes the counts to the admin topic")
    void broadcastsChangesFromIdentityService() {
        listener.onChangedElsewhere(new AdminPendingCountsChangedEvent());

        verify(messaging).convertAndSend(eq(AdminPendingCountsWebSocketListener.TOPIC), any(Object.class));
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
    @DisplayName("a broker failure never breaks the flow that triggered the push")
    void brokerFailureIsSwallowed() {
        doThrow(new IllegalStateException("broker down"))
                .when(messaging).convertAndSend(eq(AdminPendingCountsWebSocketListener.TOPIC), any(Object.class));

        assertThatCode(() -> listener.onChangedElsewhere(new AdminPendingCountsChangedEvent()))
                .doesNotThrowAnyException();
        verify(counts).getCounts();
        verify(messaging, never()).convertAndSend(eq("/topic/other"), any(Object.class));
    }
}
