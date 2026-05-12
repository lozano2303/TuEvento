package com.capysoft.tuevento.modules.event.domain.event;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventCreatedEvent {

    private Long eventId;
    private Long userId;
    private String eventName;
    private String status;
    private LocalDateTime occurredAt;
}
