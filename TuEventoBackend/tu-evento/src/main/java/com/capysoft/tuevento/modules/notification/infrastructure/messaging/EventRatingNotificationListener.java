package com.capysoft.tuevento.modules.notification.infrastructure.messaging;

import com.capysoft.tuevento.modules.event.domain.event.EventRatingAddedEvent;
import com.capysoft.tuevento.modules.event.domain.model.Event;
import com.capysoft.tuevento.modules.event.domain.repository.EventRepository;
import com.capysoft.tuevento.modules.notification.application.dto.SendNotificationCommand;
import com.capysoft.tuevento.modules.notification.application.usecase.SendNotificationUseCase;
import com.capysoft.tuevento.modules.notification.domain.model.NotificationEntityTypes;
import com.capysoft.tuevento.modules.notification.domain.model.NotificationTypeNames;
import com.capysoft.tuevento.modules.profile.infrastructure.persistence.repository.ProfileJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Sends in-app (and email) notifications triggered by new comments and replies.
 *
 * <h3>Rules</h3>
 * <ol>
 *   <li><b>NEW_COMMENT</b> — fired when a top-level comment ({@code parentRatingId == null})
 *       is posted on an event by someone other than the organizer.
 *       Recipient: the event organizer.</li>
 *   <li><b>NEW_REPLY</b> — fired when a reply ({@code parentRatingId != null}) is posted.
 *       Recipient: the user who was mentioned / replied to ({@code replyToUserId}),
 *       provided that user is not the commenter themselves.</li>
 * </ol>
 *
 * <h3>Pattern</h3>
 * Follows {@link EventStatusChangedListener}: {@code @TransactionalEventListener(AFTER_COMMIT)}
 * + {@code @Async} + {@code @Transactional(REQUIRES_NEW)} so the notification write
 * never blocks or rolls back the originating comment-save transaction.
 */
@Slf4j
@Component
public class EventRatingNotificationListener {

    private final SendNotificationUseCase sendNotificationUseCase;
    private final EventRepository         eventRepository;
    private final ProfileJpaRepository    profileJpaRepository;

    public EventRatingNotificationListener(SendNotificationUseCase sendNotificationUseCase,
                                          EventRepository eventRepository,
                                          ProfileJpaRepository profileJpaRepository) {
        this.sendNotificationUseCase = sendNotificationUseCase;
        this.eventRepository = eventRepository;
        this.profileJpaRepository = profileJpaRepository;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onEventRatingAdded(EventRatingAddedEvent event) {
        try {
            Event parentEvent = eventRepository.findById(event.getEventId()).orElse(null);
            if (parentEvent == null) {
                log.warn("[EventRatingNotificationListener] Event not found for eventId={}, skipping", event.getEventId());
                return;
            }

            Long organizerUserId = parentEvent.getUserId();
            String eventName     = parentEvent.getEventName();

            // Resolve commenter's display name
            String commenterName = profileJpaRepository.findByUserId(event.getUserId().intValue())
                    .map(p -> p.getFullName())
                    .orElse("Un usuario");

            // Truncate comment text for the notification body (max 200 chars)
            String commentSnippet = truncate(event.getComment(), 200);

            // ── Rule 1: NEW_COMMENT → notify organizer ────────────────────
            // Only for top-level comments posted by someone other than the organizer.
            if (event.getParentRatingId() == null && !Boolean.TRUE.equals(event.getIsOrganizer())) {
                notifyOrganizer(event, organizerUserId, eventName, commenterName, commentSnippet);
            }

            // ── Rule 2: NEW_REPLY → notify the mentioned / replied-to user ─
            // Only when a replyToUserId exists and it's not the commenter themselves.
            Long replyToUserId = event.getReplyToUserId();
            if (event.getParentRatingId() != null
                    && replyToUserId != null
                    && !replyToUserId.equals(event.getUserId())) {
                notifyReplyTarget(event, replyToUserId, organizerUserId, eventName, commenterName, commentSnippet);
            }

        } catch (Exception e) {
            log.error("[EventRatingNotificationListener] Unexpected error for ratingId={}, eventId={}: {}",
                    event.getRatingId(), event.getEventId(), e.getMessage(), e);
            // Never re-throw — the comment transaction already committed.
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    private void notifyOrganizer(EventRatingAddedEvent event,
                                  Long organizerUserId,
                                  String eventName,
                                  String commenterName,
                                  String commentSnippet) {
        // Use ratingId as suffix so every comment triggers a unique notification
        // even though they all reference the same entityId (the event).
        String idempotencyKey = "ratingId:" + event.getRatingId();

        SendNotificationCommand command = SendNotificationCommand.builder()
                .typeName(NotificationTypeNames.NEW_COMMENT)
                .entityType(NotificationEntityTypes.EVENT)
                .entityId(event.getEventId())
                .userIds(List.of(organizerUserId.intValue()))
                .eventName(eventName)
                .commenterName(commenterName)
                .reason(commentSnippet)          // reused as comment snippet field
                .idempotencySuffix(idempotencyKey)
                .build();

        sendNotificationUseCase.execute(command);
        log.info("[EventRatingNotificationListener] NEW_COMMENT sent to organizerUserId={} for ratingId={}",
                organizerUserId, event.getRatingId());
    }

    private void notifyReplyTarget(EventRatingAddedEvent event,
                                    Long replyToUserId,
                                    Long organizerUserId,
                                    String eventName,
                                    String commenterName,
                                    String commentSnippet) {

        // Build recipient list. Always includes replyToUserId.
        // Also include the organizer if they are the parent-comment author and
        // are different from both the commenter and replyToUserId — skip that
        // here since organizer gets a separate NEW_COMMENT for top-level only.
        List<Integer> recipients = new ArrayList<>();
        recipients.add(replyToUserId.intValue());

        String idempotencyKey = "ratingId:" + event.getRatingId() + ":reply";

        SendNotificationCommand command = SendNotificationCommand.builder()
                .typeName(NotificationTypeNames.NEW_REPLY)
                .entityType(NotificationEntityTypes.EVENT)
                .entityId(event.getEventId())
                .userIds(recipients)
                .eventName(eventName)
                .commenterName(commenterName)
                .reason(commentSnippet)
                .idempotencySuffix(idempotencyKey)
                .build();

        sendNotificationUseCase.execute(command);
        log.info("[EventRatingNotificationListener] NEW_REPLY sent to userId={} for ratingId={}",
                replyToUserId, event.getRatingId());
    }

    private static String truncate(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, max) + "…";
    }
}
