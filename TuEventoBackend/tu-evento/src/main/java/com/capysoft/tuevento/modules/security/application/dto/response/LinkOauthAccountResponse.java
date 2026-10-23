package com.capysoft.tuevento.modules.security.application.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class LinkOauthAccountResponse {

    private final Integer oauthAccountId;
    private final String provider;
    private final String email;
    private final LocalDateTime linkedAt;
}
