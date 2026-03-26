package com.capysoft.tuevento.modules.security.domain.model;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    private Integer refreshTokenId;
    private User user;
    private AuthSession authSession;
    private String token;
    private LocalDateTime issuedAt;
    private LocalDateTime expiresAt;
    private Boolean revoked;
    private LocalDateTime revokedAt;
}
