package com.capysoft.tuevento.modules.security.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents a user currently holding the ORGANIZER role.
 * Used by the admin organizer-management panel.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizerUserResponse {
    private Integer userId;
    private String alias;
    private String fullName;
    private String email;
    /** Presigned URL (60-min) for the user's profile picture. Null if unavailable. */
    private String profilePicture;
    /** Number of events this organizer has created (all statuses). */
    private Long totalEvents;
    /** Number of currently PUBLISHED events. */
    private Long publishedEvents;
    /** When this user's account was created. */
    private LocalDateTime memberSince;
}
