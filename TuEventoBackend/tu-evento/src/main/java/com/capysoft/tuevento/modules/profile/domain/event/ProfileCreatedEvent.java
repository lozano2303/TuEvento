package com.capysoft.tuevento.modules.profile.domain.event;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileCreatedEvent {

    private Long profileId;
    private Integer userId;
    private LocalDateTime occurredAt;
}
