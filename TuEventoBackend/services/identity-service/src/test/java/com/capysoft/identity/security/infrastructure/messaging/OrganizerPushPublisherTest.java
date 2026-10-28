package com.capysoft.identity.security.infrastructure.messaging;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.capysoft.identity.security.domain.event.OrganizerApprovedEvent;
import com.capysoft.identity.security.domain.event.OrganizerPetitionCreatedEvent;
import com.capysoft.identity.security.domain.event.OrganizerRejectedEvent;
import com.capysoft.identity.shared.infrastructure.messaging.LivePushSender;

@DisplayName("OrganizerPushPublisher")
class OrganizerPushPublisherTest {

    private final LivePushSender sender = mock(LivePushSender.class);
    private final OrganizerPushPublisher publisher = new OrganizerPushPublisher(sender);

    @Test
    @DisplayName("aprobar → ROLE_UPDATED al usuario y aviso de pendientes; crear y rechazar → aviso de pendientes")
    void pushes() {
        publisher.onApproved(OrganizerApprovedEvent.builder().userId(7).organizerPetitionId(3).build());
        publisher.onCreated(OrganizerPetitionCreatedEvent.builder().userId(8).build());
        publisher.onRejected(OrganizerRejectedEvent.builder().userId(9).build());

        verify(sender).send("7", "/topic/users/7/session", Map.of("type", "ROLE_UPDATED", "role", "ORGANIZER"));
        verify(sender, times(3)).send("admin", "/topic/admin/pending-counts", Map.of("type", "PENDING_COUNTS_CHANGED"));
    }
}
