package com.capysoft.identity.security.application.port.in;

import com.capysoft.identity.security.application.dto.response.RequestOrganizerResponse;

public interface GetPetitionStatusPort {
    RequestOrganizerResponse getByUserId();
}