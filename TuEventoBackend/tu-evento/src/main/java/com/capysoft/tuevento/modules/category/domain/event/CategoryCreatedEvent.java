package com.capysoft.tuevento.modules.category.domain.event;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CategoryCreatedEvent {

    private final Integer categoryId;
    private final String  name;
    private final boolean hasParent;
}
