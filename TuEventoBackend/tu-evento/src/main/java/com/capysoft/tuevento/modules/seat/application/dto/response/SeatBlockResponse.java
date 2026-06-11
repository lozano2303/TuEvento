package com.capysoft.tuevento.modules.seat.application.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SeatBlockResponse {
    private Integer seatBlockId;
    private Integer eventSectionId;
    private String name;
    private Integer capacity;
}
