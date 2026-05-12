package com.capysoft.tuevento.modules.event.application.port.in;

import com.capysoft.tuevento.modules.event.application.dto.response.EventResponse;
import com.capysoft.tuevento.modules.event.application.dto.response.EventSummaryResponse;
import com.capysoft.tuevento.modules.event.domain.model.EventStatus;

import java.util.List;

public interface GetEventUseCase {

    EventResponse findById(Long eventId);
    List<EventSummaryResponse> findByUser(Long userId);
    List<EventSummaryResponse> findByStatus(EventStatus status);
}
