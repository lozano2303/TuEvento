package com.capysoft.tuevento.modules.event.application.dto.response;

import com.capysoft.tuevento.modules.event.domain.model.EventStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class EventSummaryResponse {

    private final Long eventId;
    private final String eventName;
    private final EventStatus status;
    private final LocalDate startDate;
    private final LocalDate finishDate;
    private final boolean isPublic;
    private final int availableSeats;
}
