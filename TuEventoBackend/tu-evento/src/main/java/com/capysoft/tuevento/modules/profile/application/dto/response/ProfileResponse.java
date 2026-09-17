package com.capysoft.tuevento.modules.profile.application.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

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
    /**
     * Timestamp of the last accepted full-name change.
     * Null means the user has never changed their name (no restriction applies).
     * The next allowed change date is nameChangedAt + 14 days.
     */
    private final LocalDateTime nameChangedAt;
}
