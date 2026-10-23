package com.capysoft.tuevento.modules.security.domain.model;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginCredentials {

    private Integer loginCredentialsId;
    private User user;
    private String passwordHash;
    private String email;
    private LocalDateTime lastLoginAt;
}
