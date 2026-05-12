package com.capysoft.tuevento.modules.event.application.port.in;

import com.capysoft.tuevento.modules.event.application.dto.request.CreateEventRequest;
import com.capysoft.tuevento.modules.event.application.dto.response.EventResponse;

public interface CreateEventUseCase {

    EventResponse execute(CreateEventRequest request, Long userId);
}
