package com.capysoft.tuevento.modules.security.application.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RefreshTokenResponse {

    private final String accessToken;
    private final String refreshToken;
}
