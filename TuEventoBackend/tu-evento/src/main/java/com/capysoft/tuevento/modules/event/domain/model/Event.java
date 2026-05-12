package com.capysoft.tuevento.modules.event.domain.model;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    private Long eventId;
    private Long userId;
    private Long siteId;
    private String eventName;
    private String description;
    private LocalDate startDate;
    private LocalDate finishDate;
    private EventStatus status;
    private boolean isPublic;
    private int availableSeats;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
