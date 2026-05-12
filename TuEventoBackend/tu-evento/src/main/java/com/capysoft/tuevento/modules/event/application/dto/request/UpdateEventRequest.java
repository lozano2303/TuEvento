package com.capysoft.tuevento.modules.event.application.dto.request;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEventRequest {

    private String eventName;
    private String description;
    private Long siteId;
    private LocalDate startDate;
    private LocalDate finishDate;
    private Boolean isPublic;
    private Integer availableSeats;
}
