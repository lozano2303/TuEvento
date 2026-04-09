package com.capysoft.tuevento.modules.security.application.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RegisterUserResponse {

    private final Integer userId;
    private final String alias;
    private final String email;
}
