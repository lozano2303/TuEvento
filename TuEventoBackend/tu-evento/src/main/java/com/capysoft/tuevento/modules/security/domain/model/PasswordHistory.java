package com.capysoft.tuevento.modules.security.domain.model;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordHistory {

    private Integer passwordHistoryId;
    private User user;
    private String passwordHash;
    private LocalDateTime changedAt;
}
