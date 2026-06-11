package com.capysoft.tuevento.modules.seat.application.port.in;

import com.capysoft.tuevento.modules.seat.application.dto.request.CreateSeatRequest;
import com.capysoft.tuevento.modules.seat.application.dto.request.UpdateSeatStatusRequest;
import com.capysoft.tuevento.modules.seat.application.dto.response.SeatLogResponse;
import com.capysoft.tuevento.modules.seat.application.dto.response.SeatResponse;

import java.util.List;

public interface SeatUseCase {
    List<SeatResponse> getSeatsByBlock(Integer seatBlockId);
    List<SeatResponse> getSeatsBySection(Integer eventSectionId);
    SeatResponse createSeat(CreateSeatRequest request);
    SeatResponse updateSeatStatus(Integer seatId, UpdateSeatStatusRequest request, Integer changedBy);
    List<SeatLogResponse> getSeatLog(Integer seatId);
    void deleteSeat(Integer seatId);
}
