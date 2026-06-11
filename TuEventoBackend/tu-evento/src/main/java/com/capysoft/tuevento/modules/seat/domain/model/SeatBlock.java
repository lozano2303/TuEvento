package com.capysoft.tuevento.modules.seat.domain.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SeatBlock {
    private Integer seatBlockId;
    private Integer eventSectionId;
    private String name;
    private Integer capacity;
}
