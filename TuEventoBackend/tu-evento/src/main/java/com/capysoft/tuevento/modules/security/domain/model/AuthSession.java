package com.capysoft.tuevento.modules.security.domain.model;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthSession {

    private Integer authSessionId;
    private User user;
    private String accessToken;
    private LocalDateTime issuedAt;
    private LocalDateTime expiresAt;
    private String ipAddress;
    private String userAgent;
    private Boolean revoked;
    private LocalDateTime revokedAt;
}
