package com.capysoft.tuevento.modules.event.domain.model;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventLog {

    private Long logId;
    private Long eventId;
    private String action;
    private String oldValue;
    private String newValue;
    private LocalDateTime occurredAt;
}