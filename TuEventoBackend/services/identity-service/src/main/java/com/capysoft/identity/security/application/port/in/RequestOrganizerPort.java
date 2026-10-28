package com.capysoft.identity.security.application.port.in;

import com.capysoft.identity.security.application.dto.request.RequestOrganizerRequest;
import com.capysoft.identity.security.application.dto.response.RequestOrganizerResponse;

public interface RequestOrganizerPort {

    RequestOrganizerResponse request(RequestOrganizerRequest request);
}
