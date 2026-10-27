package com.capysoft.identity.profile.application.port.in;

import com.capysoft.identity.profile.application.dto.response.ProfileResponse;

public interface GetProfilePort {

    ProfileResponse getById(Long profileId);
}
