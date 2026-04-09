package com.capysoft.tuevento.modules.security.domain.model;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountLockout {

    private Integer accountLockoutId;
    private User user;
    private Integer failedAttempts;
    private LocalDateTime windowStartedAt;
    private LocalDateTime lockedUntil;
}
