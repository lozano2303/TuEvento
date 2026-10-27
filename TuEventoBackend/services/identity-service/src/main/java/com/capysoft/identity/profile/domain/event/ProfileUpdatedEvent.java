package com.capysoft.identity.profile.domain.event;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileUpdatedEvent {

    private Long profileId;
    private Integer userId;
    /** Nuevo idioma preferido si cambió en esta actualización; null si no cambió. */
    private Long changedLanguageId;
    private LocalDateTime occurredAt;
}
