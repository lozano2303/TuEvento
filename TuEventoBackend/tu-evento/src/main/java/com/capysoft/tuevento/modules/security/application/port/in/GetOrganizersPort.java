package com.capysoft.tuevento.modules.security.application.port.in;

import com.capysoft.tuevento.modules.security.application.dto.response.OrganizerUserResponse;

import java.util.List;

public interface GetOrganizersPort {
    /**
     * Returns all users currently holding the ORGANIZER role.
     */
    List<OrganizerUserResponse> getOrganizers();
}
