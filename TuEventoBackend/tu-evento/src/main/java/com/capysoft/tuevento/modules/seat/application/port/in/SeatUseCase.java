package com.capysoft.tuevento.modules.seat.application.port.in;

import com.capysoft.tuevento.modules.seat.application.dto.request.CreateSeatRequest;
import com.capysoft.tuevento.modules.seat.application.dto.request.UpdateSeatStatusRequest;
import com.capysoft.tuevento.modules.seat.application.dto.response.SeatLogResponse;
import com.capysoft.tuevento.modules.seat.application.dto.response.SeatResponse;

import java.util.List;

public interface SeatUseCase {
    List<SeatResponse> getSeatsByBlock(Integer seatBlockId);
    List<SeatResponse> getSeatsBySection(Integer eventSectionId);
    /** Solo el organizador del evento, con el evento en DRAFT. */
    SeatResponse createSeat(CreateSeatRequest request, Long organizerId);
    /** Cambio hecho por los flujos del sistema (pago, cancelación, reembolso): valida la transición y la publica. */
    SeatResponse updateSeatStatus(Integer seatId, UpdateSeatStatusRequest request, Integer changedBy);
    /** Venta al confirmarse el pago: solo si la silla está libre o reservada por ese comprador. */
    SeatResponse sellSeat(Integer seatId, Long buyerId, String reason);
    /** Cambio manual del organizador del evento en DRAFT: solo entre AVAILABLE y COURTESY. */
    SeatResponse updateSeatStatusByOrganizer(Integer seatId, UpdateSeatStatusRequest request, Long organizerId);
    List<SeatLogResponse> getSeatLog(Integer seatId);
    /** Solo el organizador del evento en DRAFT, y solo sillas disponibles o de cortesía. */
    void deleteSeat(Integer seatId, Long organizerId);
}
