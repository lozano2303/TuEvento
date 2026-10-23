package com.capysoft.tuevento.modules.profile.domain.model;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileLog {

    private Long logId;
    private Long profileId;
    private String action;
    private String oldValue;
    private String newValue;
    private LocalDateTime occurredAt;
}
