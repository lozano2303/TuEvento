package com.capysoft.tuevento.modules.seat.domain.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Seat {
    private Integer seatId;
    private Integer seatBlockId;
    private Integer eventSectionId;
    private String code;
    private Integer row;
    private Integer position;
    private SeatType type;
    private SeatStatus status;
    private Integer reservedBy;
    private java.time.LocalDateTime reservedUntil;
}
