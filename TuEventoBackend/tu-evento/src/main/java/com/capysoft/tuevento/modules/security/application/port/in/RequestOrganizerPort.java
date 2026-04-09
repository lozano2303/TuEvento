package com.capysoft.tuevento.modules.security.application.port.in;

import com.capysoft.tuevento.modules.security.application.dto.request.RequestOrganizerRequest;
import com.capysoft.tuevento.modules.security.application.dto.response.RequestOrganizerResponse;

public interface RequestOrganizerPort {

    RequestOrganizerResponse request(RequestOrganizerRequest request);
}
