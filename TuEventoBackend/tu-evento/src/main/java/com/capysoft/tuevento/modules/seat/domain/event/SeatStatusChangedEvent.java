package com.capysoft.tuevento.modules.seat.domain.event;

import java.time.LocalDateTime;

import com.capysoft.tuevento.modules.seat.domain.model.SeatStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SeatStatusChangedEvent {
    private Integer seatId;
    private Integer eventSectionId;
    private Integer eventId; // agregado para routing de WebSocket
    private SeatStatus oldStatus;
    private SeatStatus newStatus;
    private Integer changedBy;
    private LocalDateTime reservedUntil; // agregado para informar TTL a clientes
}
