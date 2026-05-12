package com.capysoft.tuevento.modules.event.application.port.in;

import com.capysoft.tuevento.modules.event.application.dto.request.SaveEventLayoutRequest;
import com.capysoft.tuevento.modules.event.application.dto.response.EventLayoutResponse;

public interface SaveEventLayoutUseCase {

    EventLayoutResponse execute(Long eventId, SaveEventLayoutRequest request, Long userId);
}
