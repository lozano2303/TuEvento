package com.capysoft.tuevento.modules.security.application.port.in;

import com.capysoft.tuevento.modules.security.application.dto.response.OrganizerRequestResponse;

import java.util.List;

public interface GetOrganizerRequestsPort {
    List<OrganizerRequestResponse> getPendingRequests();
}
