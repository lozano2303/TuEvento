package com.capysoft.tuevento.modules.security.domain.model;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountActivation {

    private Integer accountActivationId;
    private User user;
    private String activationCode;
    private LocalDateTime expiresAt;
    private Boolean activated;
    private LocalDateTime createdAt;
}
