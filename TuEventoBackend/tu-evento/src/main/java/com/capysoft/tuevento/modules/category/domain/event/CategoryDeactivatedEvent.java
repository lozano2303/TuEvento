package com.capysoft.tuevento.modules.category.domain.event;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CategoryDeactivatedEvent {

    private final Integer categoryId;
    private final String  name;
}
