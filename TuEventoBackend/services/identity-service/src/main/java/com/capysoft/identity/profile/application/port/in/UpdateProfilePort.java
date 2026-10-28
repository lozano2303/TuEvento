package com.capysoft.identity.profile.application.port.in;

import com.capysoft.identity.profile.application.dto.request.UpdateProfileRequest;
import com.capysoft.identity.profile.application.dto.response.ProfileResponse;

public interface UpdateProfilePort {

    ProfileResponse update(Long profileId, UpdateProfileRequest request);
}
