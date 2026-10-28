package com.capysoft.tuevento.modules.event.infrastructure.notification;

import com.capysoft.tuevento.modules.event.domain.event.EventStatusChangedEvent;
import com.capysoft.tuevento.shared.application.notification.SendNotificationCommand;
import com.capysoft.tuevento.shared.application.notification.NotificationSender;
import com.capysoft.tuevento.shared.application.notification.NotificationTypeNames;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link EventStatusChangedListener}.
 *
 * <h3>Coverage</h3>
 * <ul>
 *   <li>PENDING_REVIEW → PUBLISHED fires EVENT_PUBLISHED</li>
 *   <li>PENDING_REVIEW → REJECTED  fires EVENT_REJECTED with reason</li>
 *   <li>DRAFT → PENDING_REVIEW is silently ignored</li>
 *   <li>PUBLISHED → COMPLETED is silently ignored</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EventStatusChangedListener")
class EventStatusChangedListenerTest {

    @Mock  private NotificationSender notificationSender;
    @InjectMocks private EventStatusChangedListener listener;

    private static final Long EVENT_ID     = 7L;
    private static final Long ORGANIZER_ID = 42L;

    private EventStatusChangedEvent event(String oldStatus, String newStatus, String reason) {
        return EventStatusChangedEvent.builder()
                .eventId(EVENT_ID)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .changedBy(1L)
                .occurredAt(LocalDateTime.of(2026, 10, 5, 12, 0, 0))
                .organizerId(ORGANIZER_ID)
                .eventName("Rock Festival")
                .reason(reason)
                .build();
    }

    // ── Triggered transitions ─────────────────────────────────────────────────

    @Nested
    @DisplayName("Triggered transitions")
    class TriggeredTransitions {

        @Test
        @DisplayName("PENDING_REVIEW → PUBLISHED sends EVENT_PUBLISHED to organizer")
        void pendingReview_to_published_sends_eventPublished() {
            listener.onEventStatusChanged(event("PENDING_REVIEW", "PUBLISHED", null));

            ArgumentCaptor<SendNotificationCommand> captor =
                    ArgumentCaptor.forClass(SendNotificationCommand.class);
            verify(notificationSender).send(captor.capture());

            SendNotificationCommand cmd = captor.getValue();
            assertThat(cmd.getTypeName()).isEqualTo(NotificationTypeNames.EVENT_PUBLISHED);
            assertThat(cmd.getUserIds()).containsExactly(ORGANIZER_ID.intValue());
            assertThat(cmd.getEntityId()).isEqualTo(EVENT_ID);
            assertThat(cmd.getEventName()).isEqualTo("Rock Festival");
            assertThat(cmd.getIdempotencySuffix()).isNotBlank();
        }

        @Test
        @DisplayName("PENDING_REVIEW → REJECTED sends EVENT_REJECTED with reason to organizer")
        void pendingReview_to_rejected_sends_eventRejected() {
            listener.onEventStatusChanged(event("PENDING_REVIEW", "REJECTED", "Missing venue details"));

            ArgumentCaptor<SendNotificationCommand> captor =
                    ArgumentCaptor.forClass(SendNotificationCommand.class);
            verify(notificationSender).send(captor.capture());

            SendNotificationCommand cmd = captor.getValue();
            assertThat(cmd.getTypeName()).isEqualTo(NotificationTypeNames.EVENT_REJECTED);
            assertThat(cmd.getReason()).isEqualTo("Missing venue details");
            assertThat(cmd.getUserIds()).containsExactly(ORGANIZER_ID.intValue());
            assertThat(cmd.getIdempotencySuffix()).isNotBlank();
        }
    }

    // ── Ignored transitions ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Ignored transitions")
    class IgnoredTransitions {

        @Test
        @DisplayName("DRAFT → PENDING_REVIEW is silently ignored — no notification sent")
        void draft_to_pendingReview_ignored() {
            listener.onEventStatusChanged(event("DRAFT", "PENDING_REVIEW", null));
            verifyNoInteractions(notificationSender);
        }

        @Test
        @DisplayName("PUBLISHED → COMPLETED is silently ignored — no notification sent")
        void published_to_completed_ignored() {
            listener.onEventStatusChanged(event("PUBLISHED", "COMPLETED", null));
            verifyNoInteractions(notificationSender);
        }

        @Test
        @DisplayName("PENDING_REVIEW → DRAFT is silently ignored — only publish and reject notify")
        void pendingReview_to_draft_ignored() {
            listener.onEventStatusChanged(event("PENDING_REVIEW", "DRAFT", null));
            verifyNoInteractions(notificationSender);
        }

        @Test
        @DisplayName("without an organizer nobody is notified")
        void noOrganizer_ignored() {
            listener.onEventStatusChanged(EventStatusChangedEvent.builder().eventId(EVENT_ID)
                    .oldStatus("PENDING_REVIEW").newStatus("PUBLISHED").build());
            verifyNoInteractions(notificationSender);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("without occurredAt the idempotency suffix falls back to the current time")
        void noOccurredAt_usesCurrentTime() {
            listener.onEventStatusChanged(EventStatusChangedEvent.builder().eventId(EVENT_ID)
                    .oldStatus("PENDING_REVIEW").newStatus("REJECTED").organizerId(ORGANIZER_ID).build());

            ArgumentCaptor<SendNotificationCommand> captor = ArgumentCaptor.forClass(SendNotificationCommand.class);
            verify(notificationSender).send(captor.capture());
            assertThat(captor.getValue().getIdempotencySuffix()).matches("\\d+");
        }

        @Test
        @DisplayName("a send failure never reaches the status change")
        void sendFailure_isSwallowed() {
            doThrow(new IllegalStateException("kafka down")).when(notificationSender).send(any());

            org.assertj.core.api.Assertions.assertThatCode(() ->
                    listener.onEventStatusChanged(event("PENDING_REVIEW", "PUBLISHED", null)))
                    .doesNotThrowAnyException();
        }
    }
}
