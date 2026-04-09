package com.capysoft.tuevento.modules.profile.application.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProfileResponse {

    private final Long profileId;
    private final Integer userId;
    private final Integer cityId;
    private final String cityName;
    private final String departmentName;
    private final Integer storedFileId;
    private final String fullName;
    private final String bio;
}
