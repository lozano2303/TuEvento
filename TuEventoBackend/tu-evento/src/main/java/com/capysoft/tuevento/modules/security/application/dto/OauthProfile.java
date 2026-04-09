package com.capysoft.tuevento.modules.security.application.dto;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OauthProfile {

    private String providerUserId;
    private String email;
    private String alias;
}
