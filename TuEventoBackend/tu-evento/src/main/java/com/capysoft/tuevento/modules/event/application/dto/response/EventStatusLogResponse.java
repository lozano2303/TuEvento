package com.capysoft.tuevento.modules.event.application.dto.response;

import com.capysoft.tuevento.modules.event.domain.model.EventStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class EventStatusLogResponse {

    private final Long statusLogId;
    private final EventStatus oldStatus;
    private final EventStatus newStatus;
    private final LocalDateTime changedAt;
    private final Long changedBy;
}
