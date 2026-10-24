package com.capysoft.tuevento.modules.security.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Un admin quitó el rol de organizador a un usuario (vuelve a USER). */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizerRoleRevokedEvent {

    private Integer userId;
    private Integer revokedBy;
    private String reason;
    private LocalDateTime occurredAt;
}
