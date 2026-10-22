package com.capysoft.tuevento.modules.security.application.port.in;

import com.capysoft.tuevento.modules.security.application.dto.response.OrganizerRequestResponse;

import java.util.List;

public interface GetOrganizerRequestsPort {
    /**
     * Returns organizer requests filtered by status.
     * Pass {@code null} to retrieve all statuses.
     */
    List<OrganizerRequestResponse> getRequests(String status);
}
