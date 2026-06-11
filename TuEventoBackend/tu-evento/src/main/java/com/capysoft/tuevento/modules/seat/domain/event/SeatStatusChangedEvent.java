package com.capysoft.tuevento.modules.seat.domain.event;

import com.capysoft.tuevento.modules.seat.domain.model.SeatStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SeatStatusChangedEvent {
    private Integer seatId;
    private Integer eventSectionId;
    private SeatStatus oldStatus;
    private SeatStatus newStatus;
    private Integer changedBy;
}
