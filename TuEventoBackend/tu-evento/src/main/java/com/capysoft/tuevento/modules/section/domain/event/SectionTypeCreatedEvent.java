package com.capysoft.tuevento.modules.section.domain.event;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SectionTypeCreatedEvent {

    private final Integer sectionTypeId;
    private final String name;
}
