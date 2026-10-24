package com.capysoft.tuevento.modules.security.application.port.in;

public interface RevokeOrganizerRolePort {
    /**
     * Revokes the ORGANIZER role from the given user and downgrades them to USER.
     * Fires {@link com.capysoft.tuevento.modules.security.domain.event.OrganizerRoleRevokedEvent}
     * and pushes a WebSocket notification to the affected user. The user's published
     * events are cancelled and the ones in review are rejected.
     *
     * @param userId      the ID of the user whose role is being revoked
     * @param reason      why the role is revoked (min 10 chars); notified to the user
     * @param adminUserId the ADMIN performing the action (audit trail)
     */
    void revoke(Integer userId, String reason, Integer adminUserId);
}
