package com.capysoft.identity.profile.application.port.in;

import com.capysoft.identity.profile.application.dto.request.CreateProfileRequest;
import com.capysoft.identity.profile.application.dto.response.ProfileResponse;

public interface CreateProfilePort {

    ProfileResponse create(CreateProfileRequest request);
}
