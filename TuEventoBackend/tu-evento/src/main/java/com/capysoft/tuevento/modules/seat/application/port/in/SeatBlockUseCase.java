package com.capysoft.tuevento.modules.seat.application.port.in;

import com.capysoft.tuevento.modules.seat.application.dto.request.CreateSeatBlockRequest;
import com.capysoft.tuevento.modules.seat.application.dto.response.SeatBlockResponse;

import java.util.List;

public interface SeatBlockUseCase {
    List<SeatBlockResponse> getBlocksBySection(Integer eventSectionId);
    SeatBlockResponse createSeatBlock(CreateSeatBlockRequest request);
    void deleteSeatBlock(Integer seatBlockId);
}
