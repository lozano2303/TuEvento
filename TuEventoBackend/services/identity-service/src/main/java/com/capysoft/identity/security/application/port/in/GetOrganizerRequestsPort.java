package com.capysoft.identity.security.application.port.in;

import com.capysoft.identity.security.application.dto.response.OrganizerRequestResponse;

import java.util.List;

public interface GetOrganizerRequestsPort {
    /**
     * Returns organizer requests filtered by status.
     * Pass {@code null} to retrieve all statuses.
     *
     * @param adminUserId the ADMIN listing the requests (used for storage access and audit)
     */
    List<OrganizerRequestResponse> getRequests(String status, Integer adminUserId);
}
