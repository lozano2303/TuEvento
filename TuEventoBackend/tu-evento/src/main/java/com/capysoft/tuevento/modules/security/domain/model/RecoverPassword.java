package com.capysoft.tuevento.modules.security.domain.model;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecoverPassword {

    private Integer recoverPasswordId;
    private User user;
    private String code;
    private Boolean codeStatus;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private String lastPasswordHash;
}
