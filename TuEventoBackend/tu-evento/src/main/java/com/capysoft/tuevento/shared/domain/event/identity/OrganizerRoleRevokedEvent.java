package com.capysoft.tuevento.shared.domain.event.identity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Un admin quitó el rol de organizador a un usuario (vuelve a USER). */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizerRoleRevokedEvent {

    /** ID of the user whose ORGANIZER role was revoked. */
    private Integer userId;

    /** Human-readable alias of the affected user. */
    private String alias;

    /** ADMIN who revoked the role. */
    private Integer revokedBy;

    /** Reason written by the admin; notified to the user. */
    private String reason;

    /** When the revocation happened. */
    private LocalDateTime occurredAt;
}
