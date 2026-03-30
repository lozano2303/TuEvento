package com.capysoft.tuevento.modules.security.application.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {

    private final String accessToken;
    private final String refreshToken;
    private final Integer userId;
    private final String alias;
}
