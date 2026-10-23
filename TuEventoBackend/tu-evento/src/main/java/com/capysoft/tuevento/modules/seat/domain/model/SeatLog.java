package com.capysoft.tuevento.modules.seat.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SeatLog {
    private Integer seatLogId;
    private Integer seatId;
    private SeatStatus oldStatus;
    private SeatStatus newStatus;
    private LocalDateTime changedAt;
    private Integer changedBy;
    private String reason;
}
