package com.capysoft.tuevento.modules.category.domain.event;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CategoryAssignedToEventEvent {

    private final Integer categoryEventId;
    private final Integer categoryId;
    private final Integer eventId;
}
