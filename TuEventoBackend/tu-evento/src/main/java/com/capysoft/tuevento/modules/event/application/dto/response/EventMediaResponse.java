package com.capysoft.tuevento.modules.event.application.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EventMediaResponse {

    private final Long mediaId;
    private final Long eventId;
    private final String imgUrl;
}
