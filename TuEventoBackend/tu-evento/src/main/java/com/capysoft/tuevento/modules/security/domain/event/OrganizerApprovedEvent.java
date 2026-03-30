package com.capysoft.tuevento.modules.security.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizerApprovedEvent {

    private Integer organizerPetitionId;
    private Integer userId;
    private String alias;
    private LocalDateTime occurredAt;
}
