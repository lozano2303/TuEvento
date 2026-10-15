package com.capysoft.tuevento.modules.event.domain.event;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventUpdatedEvent {

    private Long eventId;
    private Integer userId;
    private LocalDateTime occurredAt;
}