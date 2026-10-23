package com.capysoft.tuevento.modules.event.application.port.in;

import com.capysoft.tuevento.modules.event.application.dto.request.ChangeEventStatusRequest;
import com.capysoft.tuevento.modules.event.application.dto.response.EventStatusLogResponse;

public interface ChangeEventStatusUseCase {

    EventStatusLogResponse execute(Long eventId, ChangeEventStatusRequest request, Long userId);
}
