package com.capysoft.tuevento.modules.security.domain.model;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountReactivationToken {

    private Integer reactivationTokenId;
    private User    user;
    private String  token;
    /** false = pending/active, true = used or invalidated */
    private Boolean tokenStatus;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
