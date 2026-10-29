package com.capysoft.tuevento.modules.ticket.infrastructure.client;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.capysoft.tuevento.modules.seat.application.dto.request.UpdateSeatStatusRequest;
import com.capysoft.tuevento.modules.seat.application.port.in.SeatUseCase;
import com.capysoft.tuevento.modules.seat.domain.model.Seat;
import com.capysoft.tuevento.modules.seat.domain.model.SeatStatus;
import com.capysoft.tuevento.modules.seat.domain.repository.SeatRepository;
import com.capysoft.tuevento.modules.section.domain.exception.EventSectionNotFoundException;
import com.capysoft.tuevento.modules.section.domain.model.EventSection;
import com.capysoft.tuevento.modules.section.domain.repository.EventSectionRepository;
import com.capysoft.tuevento.modules.ticket.application.port.out.CatalogSeatsPort;

import lombok.RequiredArgsConstructor;

/**
 * Único punto de Comercio que toca las sillas de Catálogo. Corre dentro de la transacción del
 * caso de uso que lo llama: si algo falla, se deshace también la orden.
 */
@Component
@RequiredArgsConstructor
class CatalogSeatsAdapter implements CatalogSeatsPort {

    private static final String CURRENCY = "COP";

    private final SeatRepository seatRepository;
    private final EventSectionRepository eventSectionRepository;
    private final SeatUseCase seatUseCase;

    @Override
    public HeldSeats holdForOrder(Long orderId, Long userId, Long eventId, List<Integer> seatIds) {
        // orderId se usará para ligar la reserva a la orden (seat.pending_order_id, C0.4)
        List<Seat> seats = new ArrayList<>();
        for (Integer seatId : seatIds) {
            Seat seat = seatRepository.findByIdForUpdate(seatId)
                .orElseThrow(() -> new IllegalArgumentException("Seat not found: " + seatId));

            // Sin el estado, una silla ya vendida que conservara reservedBy pasaría la validación
            if (seat.getStatus() != SeatStatus.RESERVED) {
                throw new IllegalStateException("Seat " + seatId + " is not reserved (status " + seat.getStatus() + ")");
            }
            if (seat.getReservedBy() == null || !seat.getReservedBy().equals(userId.intValue())) {
                throw new IllegalStateException("Seat " + seatId + " is not reserved by this user");
            }
            if (seat.getReservedUntil() == null || LocalDateTime.now().isAfter(seat.getReservedUntil())) {
                throw new IllegalStateException("Seat " + seatId + " reservation has expired");
            }
            seats.add(seat);
        }

        // Un precio por sección; todas las sillas deben ser del evento de la orden: si no, sus
        // entradas y su dinero quedarían atribuidos a otro evento
        Map<Integer, BigDecimal> sectionPrices = new HashMap<>();
        for (Seat seat : seats) {
            sectionPrices.computeIfAbsent(seat.getEventSectionId(), sectionId -> {
                EventSection section = eventSectionRepository.findById(sectionId)
                    .orElseThrow(() -> new EventSectionNotFoundException(sectionId));
                if (!section.getEventId().equals(Math.toIntExact(eventId))) {
                    throw new IllegalStateException("Seat section " + sectionId + " does not belong to event " + eventId);
                }
                return section.getPrice();
            });
        }

        return new HeldSeats(CURRENCY, seats.stream()
            .map(s -> new HeldSeat(s.getSeatId(), s.getEventSectionId(), sectionPrices.get(s.getEventSectionId())))
            .toList());
    }

    @Override
    public void sell(Long orderId, Long buyerId, List<Integer> seatIds, String reason) {
        // Si alguna la tomó otra persona mientras tanto, falla y se deshace toda la confirmación
        for (Integer seatId : seatIds) {
            seatUseCase.sellSeat(seatId, buyerId, reason);
        }
    }

    @Override
    public void release(Long orderId, List<Integer> seatIds, Integer changedBy, String reason) {
        for (Integer seatId : seatIds) {
            UpdateSeatStatusRequest request = UpdateSeatStatusRequest.builder()
                .newStatus(SeatStatus.AVAILABLE)
                .reason(reason)
                .build();
            seatUseCase.updateSeatStatus(seatId, request, changedBy);
        }
    }
}
