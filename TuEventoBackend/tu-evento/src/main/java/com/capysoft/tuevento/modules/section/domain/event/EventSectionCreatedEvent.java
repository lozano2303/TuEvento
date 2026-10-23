package com.capysoft.tuevento.modules.section.domain.event;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class EventSectionCreatedEvent {
    private Integer eventSectionId;
    private Integer eventId;
    private Integer sectionTypeId;
    private Integer capacity;
    private BigDecimal price;
}
