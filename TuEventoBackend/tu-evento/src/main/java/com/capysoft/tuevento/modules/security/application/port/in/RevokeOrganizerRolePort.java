package com.capysoft.tuevento.modules.security.application.port.in;

public interface RevokeOrganizerRolePort {
    /**
     * Revokes the ORGANIZER role from the given user and downgrades them to USER.
     * Fires {@link com.capysoft.tuevento.modules.security.domain.event.OrganizerRoleRevokedEvent}
     * and pushes a WebSocket notification to the affected user.
     *
     * @param userId the ID of the user whose role is being revoked
     */
    void revoke(Integer userId);
}
