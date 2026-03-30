package com.capysoft.tuevento.modules.security.domain.event;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizerPetitionCreatedEvent {

    private Integer userId;
    private Integer organizerPetitionId;
    private LocalDateTime occurredAt;
}
