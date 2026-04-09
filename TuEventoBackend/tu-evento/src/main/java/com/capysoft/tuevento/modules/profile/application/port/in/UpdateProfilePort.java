package com.capysoft.tuevento.modules.profile.application.port.in;

import com.capysoft.tuevento.modules.profile.application.dto.request.UpdateProfileRequest;
import com.capysoft.tuevento.modules.profile.application.dto.response.ProfileResponse;

public interface UpdateProfilePort {

    ProfileResponse update(Long profileId, UpdateProfileRequest request);
}
