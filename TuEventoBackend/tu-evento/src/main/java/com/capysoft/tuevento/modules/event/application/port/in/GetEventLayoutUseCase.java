package com.capysoft.tuevento.modules.event.application.port.in;

import com.capysoft.tuevento.modules.event.application.dto.response.EventLayoutResponse;

public interface GetEventLayoutUseCase {

    EventLayoutResponse findByEventId(Long eventId);
}
