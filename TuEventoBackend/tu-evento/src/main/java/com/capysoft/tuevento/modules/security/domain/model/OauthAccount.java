package com.capysoft.tuevento.modules.security.domain.model;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OauthAccount {

    private Integer oauthAccountId;
    private User user;
    private String provider;
    private String providerUserId;
    private String email;
    private LocalDateTime linkedAt;
}
