package com.capysoft.tuevento.modules.profile.application.port.in;

import com.capysoft.tuevento.modules.profile.application.dto.response.ProfileResponse;

public interface GetProfileByUserIdPort {

    ProfileResponse getByUserId(Integer userId);
}
