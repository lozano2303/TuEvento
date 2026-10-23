package com.capysoft.tuevento.modules.seat.application.dto.response;

import com.capysoft.tuevento.modules.seat.domain.model.SeatStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SeatLogResponse {
    private Integer seatLogId;
    private Integer seatId;
    private SeatStatus oldStatus;
    private SeatStatus newStatus;
    private LocalDateTime changedAt;
    private Integer changedBy;
    private String reason;
}
