package com.capysoft.tuevento.modules.section.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class EventSection {
    private Integer eventSectionId;
    private Integer eventId;
    private Integer sectionTypeId;
    private Integer capacity;
    private Integer availableSeats;
    private BigDecimal price;
    private Boolean isActive;
}
