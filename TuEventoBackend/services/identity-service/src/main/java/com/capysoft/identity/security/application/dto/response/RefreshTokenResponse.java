package com.capysoft.identity.security.application.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class RefreshTokenResponse {

    private final String accessToken;
    private final String refreshToken;
}
