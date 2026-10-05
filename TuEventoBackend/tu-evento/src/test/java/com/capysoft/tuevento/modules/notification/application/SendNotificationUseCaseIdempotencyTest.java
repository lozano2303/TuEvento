package com.capysoft.tuevento.modules.notification.application;

import com.capysoft.tuevento.modules.notification.application.dto.SendNotificationCommand;
import com.capysoft.tuevento.modules.notification.application.port.out.NotificationChannelPort;
import com.capysoft.tuevento.modules.notification.application.usecase.SendNotificationUseCase;
import com.capysoft.tuevento.modules.notification.domain.model.Channel;
import com.capysoft.tuevento.modules.notification.domain.model.Notification;
import com.capysoft.tuevento.modules.notification.domain.model.NotificationChannelNames;
import com.capysoft.tuevento.modules.notification.domain.model.NotificationType;
import com.capysoft.tuevento.modules.notification.domain.model.NotificationTypeNames;
import com.capysoft.tuevento.modules.notification.domain.repository.ChannelRepository;
import com.capysoft.tuevento.modules.notification.domain.repository.NotificationRepository;
import com.capysoft.tuevento.modules.notification.domain.repository.NotificationTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Idempotency tests for {@link SendNotificationUseCase}.
 *
 * <h3>Coverage</h3>
 * <ul>
 *   <li>Two EVENT_REJECTED commands with different idempotencySuffix values
 *       (simulating two rejection cycles) each produce a notification.</li>
 *   <li>Two identical EVENT_REJECTED commands with the same idempotencySuffix
 *       produce only one notification (deduplication).</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SendNotificationUseCase — idempotency with suffix")
class SendNotificationUseCaseIdempotencyTest {

    @Mock private ChannelRepository          channelRepository;
    @Mock private NotificationTypeRepository notificationTypeRepository;
    @Mock private NotificationRepository     notificationRepository;
    @Mock private NotificationChannelPort    inAppPort;

    private SendNotificationUseCase useCase;

    private static final Long EVENT_ID = 20L;

    @BeforeEach
    void setUp() {
        when(inAppPort.channelName()).thenReturn(NotificationChannelNames.IN_APP);

        useCase = new SendNotificationUseCase(
                channelRepository,
                notificationTypeRepository,
                notificationRepository,
                List.of(inAppPort),
                false  // email disabled for these tests
        );

        // Stub an active notification type
        NotificationType type = NotificationType.builder()
                .notificationTypeId(5L)
                .name(NotificationTypeNames.EVENT_REJECTED)
                .active(true)
                .build();
        when(notificationTypeRepository.findByName(NotificationTypeNames.EVENT_REJECTED))
                .thenReturn(Optional.of(type));

        // Stub active channels — only IN_APP
        Channel inApp = Channel.builder()
                .channelId(1L)
                .name(NotificationChannelNames.IN_APP)
                .active(true)
                .build();
        when(channelRepository.findAllActive()).thenReturn(List.of(inApp));

        // Stub repository save to return a non-null Notification
        when(notificationRepository.save(any())).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            return Notification.builder()
                    .notificationId(99L)
                    .channelId(n.getChannelId())
                    .notificationTypeId(n.getNotificationTypeId())
                    .entityType(n.getEntityType())
                    .entityId(n.getEntityId())
                    .subject(n.getSubject())
                    .body(n.getBody())
                    .sentAt(n.getSentAt())
                    .idempotencyKey(n.getIdempotencyKey())
                    .build();
        });
    }

    @Test
    @DisplayName("Two rejection cycles with different suffixes both produce a notification")
    void two_rejection_cycles_produce_two_notifications() {
        // First rejection — suffix = timestamp of first cycle
        when(notificationRepository.existsByIdempotencyKey(
                NotificationTypeNames.EVENT_REJECTED + ":" + EVENT_ID + ":IN_APP:2026-10-01T10:00"))
                .thenReturn(false);

        useCase.execute(rejectedCmd("Not enough images", "2026-10-01T10:00"));

        // Second rejection — suffix = timestamp of second cycle (different)
        when(notificationRepository.existsByIdempotencyKey(
                NotificationTypeNames.EVENT_REJECTED + ":" + EVENT_ID + ":IN_APP:2026-10-03T15:30"))
                .thenReturn(false);

        useCase.execute(rejectedCmd("Missing venue details", "2026-10-03T15:30"));

        // Both commands delivered — adapter called twice
        verify(inAppPort, times(2)).deliver(any(), any());
    }

    @Test
    @DisplayName("Duplicate command with same suffix delivers only once")
    void duplicate_command_same_suffix_delivers_once() {
        String suffix = "2026-10-01T10:00";
        String key    = NotificationTypeNames.EVENT_REJECTED + ":" + EVENT_ID + ":IN_APP:" + suffix;

        // First call: key not yet in DB
        when(notificationRepository.existsByIdempotencyKey(key)).thenReturn(false);
        useCase.execute(rejectedCmd("Not enough images", suffix));

        // Second call: key now exists
        when(notificationRepository.existsByIdempotencyKey(key)).thenReturn(true);
        useCase.execute(rejectedCmd("Not enough images", suffix));

        // Adapter called only once — second was deduplicated
        verify(inAppPort, times(1)).deliver(any(), any());
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private SendNotificationCommand rejectedCmd(String reason, String suffix) {
        return SendNotificationCommand.builder()
                .typeName(NotificationTypeNames.EVENT_REJECTED)
                .entityType("EVENT")
                .entityId(EVENT_ID)
                .userIds(List.of(42))
                .eventName("Rock Festival 2027")
                .reason(reason)
                .idempotencySuffix(suffix)
                .build();
    }
}
