package com.capysoft.tuevento.modules.event.application.port.in;

import com.capysoft.tuevento.modules.event.application.dto.request.UpdateEventRequest;
import com.capysoft.tuevento.modules.event.application.dto.response.EventResponse;

public interface UpdateEventUseCase {

    EventResponse execute(Long eventId, UpdateEventRequest request, Long userId);
}
