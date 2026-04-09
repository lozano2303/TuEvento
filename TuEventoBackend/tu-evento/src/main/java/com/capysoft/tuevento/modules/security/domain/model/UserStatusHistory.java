package com.capysoft.tuevento.modules.security.domain.model;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatusHistory {

    private Integer userStatusHistoryId;
    private User user;
    private UserStatus userStatus;
    private String reason;
}
