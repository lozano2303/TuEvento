package com.capysoft.tuevento.shared.domain.event.identity;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserActivatedEvent {

    private Integer userId;
    private String alias;
    private LocalDateTime occurredAt;
}
