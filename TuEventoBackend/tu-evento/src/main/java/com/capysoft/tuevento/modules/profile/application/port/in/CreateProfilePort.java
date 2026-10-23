package com.capysoft.tuevento.modules.profile.application.port.in;

import com.capysoft.tuevento.modules.profile.application.dto.request.CreateProfileRequest;
import com.capysoft.tuevento.modules.profile.application.dto.response.ProfileResponse;

public interface CreateProfilePort {

    ProfileResponse create(CreateProfileRequest request);
}
