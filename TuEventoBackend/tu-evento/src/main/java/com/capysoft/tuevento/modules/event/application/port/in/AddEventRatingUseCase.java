package com.capysoft.tuevento.modules.event.application.port.in;

import com.capysoft.tuevento.modules.event.application.dto.request.AddEventRatingRequest;
import com.capysoft.tuevento.modules.event.application.dto.response.EventRatingResponse;

public interface AddEventRatingUseCase {

    EventRatingResponse execute(Long eventId, AddEventRatingRequest request, Long userId);
}
