package com.capysoft.tuevento.modules.security.domain.event;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLockedEvent {

    private Integer userId;
    private String alias;
    private LocalDateTime lockedUntil;
    private LocalDateTime occurredAt;
}
