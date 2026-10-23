package com.capysoft.tuevento.modules.event.application.dto.response;

import com.capysoft.tuevento.modules.event.domain.model.EventStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

/**
 * Richer summary used exclusively by admin endpoints.
 * Extends EventSummaryResponse data with organizer name and category label
 * so the admin table can display them without additional frontend requests.
 */
@Getter
@Builder
public class AdminEventSummaryResponse {

    private final Long        eventId;
    private final String      eventName;
    private final EventStatus status;
    private final LocalDate   startDate;
    private final LocalDate   finishDate;
    private final Boolean     isPublic;
    private final int         availableSeats;
    private final String      siteName;
    private final Integer     categoryId;
    private final String      categoryName;
    /** Presigned / public URL of the first uploaded image for this event. */
    private final String      coverUrl;
    /** Full name of the organizer who owns this event (from their profile). */
    private final String      organizerName;
    /** userId of the organizer — kept for linking to their profile if needed. */
    private final Long        organizerUserId;
    /** Email of the organizer (from login_credentials). */
    private final String      organizerEmail;
    /** Presigned URL (60-min) of the organizer's profile picture avatar. Null if unavailable. */
    private final String      organizerProfilePicture;
}
