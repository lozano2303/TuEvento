package com.capysoft.tuevento.modules.category.domain.event;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CategoryAssignedToEventEvent {

    private final Long categoryEventId;
    private final Long categoryId;
    private final Long eventId;
}
