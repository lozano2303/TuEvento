package com.capysoft.tuevento.modules.event.domain.model;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventStatusLog {

    private Long statusLogId;
    private Long eventId;
    private EventStatus oldStatus;
    private EventStatus newStatus;
    private LocalDateTime changedAt;
    private Long changedBy;
    /** Stores the admin rejection reason when newStatus is REJECTED; null otherwise. */
    private String reason;
}
