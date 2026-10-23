package com.capysoft.tuevento.modules.section.application.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class EventSectionResponse {
    private Integer eventSectionId;
    private Integer eventId;
    private String sectionTypeName;
    private Integer capacity;
    private Integer availableSeats;
    private BigDecimal price;
    private Boolean isActive;
}
