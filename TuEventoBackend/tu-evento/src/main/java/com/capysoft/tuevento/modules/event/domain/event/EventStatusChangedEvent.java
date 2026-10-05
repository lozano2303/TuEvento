package com.capysoft.tuevento.modules.event.domain.event;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Domain event published after every successful event-status transition.
 *
 * <p>Carries all data a downstream listener needs so that the notification
 * module never has to query the event repository directly (same pattern as
 * {@code PaymentApprovedEvent}).
 *
 * <ul>
 *   <li>{@code organizerId} — userId of the event owner (not the actor who changed the status)</li>
 *   <li>{@code eventName}   — human-readable title, used in notification copy</li>
 *   <li>{@code reason}      — rejection reason when newStatus=REJECTED; null otherwise</li>
 * </ul>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventStatusChangedEvent {

    private Long          eventId;
    private String        oldStatus;
    private String        newStatus;
    private Long          changedBy;
    private LocalDateTime occurredAt;

    /** userId of the event owner — needed to route the notification to the organizer. */
    private Long   organizerId;
    /** Human-readable event name — included in notification copy. */
    private String eventName;
    /**
     * Rejection reason — populated only when {@code newStatus} is {@code REJECTED};
     * null for all other transitions.
     */
    private String reason;
}
