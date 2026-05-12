package com.capysoft.tuevento.modules.event.domain.event;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventStatusChangedEvent {

    private Long eventId;
    private String oldStatus;
    private String newStatus;
    private Long changedBy;
    private LocalDateTime occurredAt;
}
