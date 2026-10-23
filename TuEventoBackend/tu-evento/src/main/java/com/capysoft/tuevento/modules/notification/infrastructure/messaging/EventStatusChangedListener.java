package com.capysoft.tuevento.modules.notification.infrastructure.messaging;

import com.capysoft.tuevento.modules.event.domain.event.EventStatusChangedEvent;
import com.capysoft.tuevento.modules.notification.application.dto.SendNotificationCommand;
import com.capysoft.tuevento.modules.notification.application.usecase.SendNotificationUseCase;
import com.capysoft.tuevento.modules.notification.domain.model.NotificationEntityTypes;
import com.capysoft.tuevento.modules.notification.domain.model.NotificationTypeNames;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/**
 * Sends notifications to the event organizer when their event exits admin review.
 *
 * <h3>Triggered transitions</h3>
 * <ul>
 *   <li>{@code PENDING_REVIEW → PUBLISHED} — sends {@code EVENT_PUBLISHED}</li>
 *   <li>{@code PENDING_REVIEW → REJECTED}  — sends {@code EVENT_REJECTED} with reason</li>
 * </ul>
 * All other transitions (e.g. DRAFT→PENDING_REVIEW, PUBLISHED→CANCELLED,
 * scheduler-driven PUBLISHED→COMPLETED) are silently ignored.
 *
 * <h3>Idempotency</h3>
 * The idempotency key includes {@code occurredAt} as a suffix so that a single
 * event can be rejected, corrected, resubmitted and rejected again without
 * the second notification being suppressed.
 *
 * <h3>Pattern</h3>
 * Follows {@link PaymentEventListener}: {@code @TransactionalEventListener(AFTER_COMMIT)}
 * + {@code @Async} + {@code @Transactional(REQUIRES_NEW)} so the notification
 * write never blocks or rolls back the originating status-change transaction.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventStatusChangedListener {

    private static final String PENDING_REVIEW = "PENDING_REVIEW";
    private static final String PUBLISHED      = "PUBLISHED";
    private static final String REJECTED       = "REJECTED";

    private final SendNotificationUseCase sendNotificationUseCase;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onEventStatusChanged(EventStatusChangedEvent event) {

        // Only react to the two review-exit transitions.
        if (!PENDING_REVIEW.equals(event.getOldStatus())) {
            return;
        }
        if (!PUBLISHED.equals(event.getNewStatus()) && !REJECTED.equals(event.getNewStatus())) {
            return;
        }
        if (event.getOrganizerId() == null) {
            log.warn("[EventStatusChangedListener] organizerId is null for eventId={}, skipping", event.getEventId());
            return;
        }

        String typeName = PUBLISHED.equals(event.getNewStatus())
                ? NotificationTypeNames.EVENT_PUBLISHED
                : NotificationTypeNames.EVENT_REJECTED;

        // Include occurredAt in the key so the same event can be rejected more than
        // once (across re-submission cycles) without being silently deduplicated.
        String idempotencySuffix = event.getOccurredAt() != null
                ? event.getOccurredAt().toString()
                : String.valueOf(System.currentTimeMillis());

        try {
            log.debug("[EventStatusChangedListener] Processing {} for eventId={}, organizerId={}",
                    typeName, event.getEventId(), event.getOrganizerId());

            SendNotificationCommand command = SendNotificationCommand.builder()
                    .typeName(typeName)
                    .entityType(NotificationEntityTypes.EVENT)
                    .entityId(event.getEventId())
                    .userIds(List.of(event.getOrganizerId().intValue()))
                    .eventName(event.getEventName())
                    .reason(event.getReason())
                    .idempotencySuffix(idempotencySuffix)
                    .build();

            sendNotificationUseCase.execute(command);

            log.info("[EventStatusChangedListener] Notification {} sent for eventId={}, organizerId={}",
                    typeName, event.getEventId(), event.getOrganizerId());

        } catch (Exception e) {
            log.error("[EventStatusChangedListener] Failed to send {} for eventId={}: {}",
                    typeName, event.getEventId(), e.getMessage(), e);
            // Never re-throw — the status-change transaction already committed.
        }
    }
}
