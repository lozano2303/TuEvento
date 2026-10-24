package com.capysoft.tuevento.modules.security.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizerRoleRevokedEvent {

    /** ID of the user whose ORGANIZER role was revoked. */
    private Integer userId;

    /** Human-readable alias of the affected user. */
    private String alias;

    /** When the revocation happened. */
    private LocalDateTime occurredAt;
}
