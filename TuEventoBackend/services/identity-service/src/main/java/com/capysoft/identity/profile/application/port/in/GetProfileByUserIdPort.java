package com.capysoft.identity.profile.application.port.in;

import com.capysoft.identity.profile.application.dto.response.ProfileResponse;

public interface GetProfileByUserIdPort {

    ProfileResponse getByUserId(Integer userId);
    
    ProfileResponse getByUserId(Integer userId, String languageCode);
}
