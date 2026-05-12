package com.capysoft.tuevento.modules.event.application.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EventLayoutResponse {

    private final Long eventLayoutId;
    private final Long eventId;
    private final String layoutData;
}
