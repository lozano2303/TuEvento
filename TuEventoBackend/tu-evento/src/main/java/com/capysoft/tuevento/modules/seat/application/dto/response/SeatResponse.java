package com.capysoft.tuevento.modules.seat.application.dto.response;

import com.capysoft.tuevento.modules.seat.domain.model.SeatStatus;
import com.capysoft.tuevento.modules.seat.domain.model.SeatType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SeatResponse {
    private Integer seatId;
    private Integer seatBlockId;
    private Integer eventSectionId;
    private String code;
    private Integer row;
    private Integer position;
    private SeatType type;
    private SeatStatus status;
    private Integer reservedBy;
    private LocalDateTime reservedUntil;
}
