package com.capysoft.tuevento.modules.security.domain.event;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordChangedEvent {

    private Integer userId;
    private LocalDateTime occurredAt;
}
