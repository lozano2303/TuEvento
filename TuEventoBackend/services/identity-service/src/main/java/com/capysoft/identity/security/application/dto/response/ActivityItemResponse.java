package com.capysoft.identity.security.application.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Represents a single activity entry in the admin activity history feed.
 * Combines events, payments and user registrations into a unified timeline.
 */
@Getter
@Builder
public class ActivityItemResponse {

    /** Discriminator: EVENT_STATUS_CHANGE | USER_REGISTERED | PAYMENT_APPROVED | REFUND_REQUESTED */
    private final String          type;

    /** Human-readable title, e.g. "Evento aprobado y publicado" */
    private final String          title;

    /** Short description / sub-line, e.g. event name or user email */
    private final String          description;

    /** Actor who performed the action (admin name, organizer name, user alias…) */
    private final String          actor;

    /** Timestamp of the activity */
    private final LocalDateTime   occurredAt;
}
