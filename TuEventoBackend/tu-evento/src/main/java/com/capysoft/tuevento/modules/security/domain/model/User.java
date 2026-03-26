package com.capysoft.tuevento.modules.security.domain.model;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private Integer userId;
    private UserStatus userStatus;
    private Role role;
    private String alias;
    private Boolean activated;
}
