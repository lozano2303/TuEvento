package com.capysoft.tuevento.modules.catalogbridge.application;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.capysoft.tuevento.modules.seat.domain.model.Seat;
import com.capysoft.tuevento.modules.seat.domain.model.SeatStatus;
import com.capysoft.tuevento.modules.seat.domain.repository.SeatRepository;
import com.capysoft.tuevento.modules.section.domain.model.EventSection;
import com.capysoft.tuevento.modules.section.domain.repository.EventSectionRepository;

import lombok.RequiredArgsConstructor;

/**
 * Paso 1 de la saga de compra: comprueba que las sillas sigan reservadas por el comprador y sean del
 * evento, y devuelve su precio. Lee con lock pesimista, en orden de seatId, para que dos órdenes con
 * sillas cruzadas no se interbloqueen.
 *
 * <p>Aún no liga la reserva a la orden (seat.pending_order_id, C0.4): la reserva sigue protegida
 * como hasta ahora, por su plazo y por la orden en PAYMENT_PENDING.
 */
@Service
@RequiredArgsConstructor
public class HoldSeatsForOrderService {

    static final String CURRENCY = "COP";

    private final SeatRepository seatRepository;
    private final EventSectionRepository eventSectionRepository;

    public record HeldSeat(Integer seatId, Integer eventSectionId, BigDecimal price) {
    }

    public record HeldSeats(String currency, List<HeldSeat> seats) {
    }

    /** No se puede bloquear: {@code code} es el del contrato (SEAT_NOT_RESERVED, SEAT_RESERVATION_EXPIRED...). */
    public static class HoldRejectedException extends RuntimeException {

        private final String code;

        public HoldRejectedException(String code, String message) {
            super(message);
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }

    @Transactional
    public HeldSeats hold(Long userId, Long eventId, List<Integer> seatIds) {
        List<Seat> seats = new ArrayList<>();
        for (Integer seatId : seatIds.stream().distinct().sorted().toList()) {
            Seat seat = seatRepository.findByIdForUpdate(seatId)
                    .orElseThrow(() -> new HoldRejectedException("SEAT_NOT_FOUND", "Seat not found: " + seatId));
            // Sin el estado, una silla ya vendida que conservara reservedBy pasaría la validación
            if (seat.getStatus() != SeatStatus.RESERVED) {
                throw new HoldRejectedException("SEAT_NOT_RESERVED",
                        "Seat " + seatId + " is not reserved (status " + seat.getStatus() + ")");
            }
            if (seat.getReservedBy() == null || !seat.getReservedBy().equals(userId.intValue())) {
                throw new HoldRejectedException("SEAT_NOT_RESERVED", "Seat " + seatId + " is not reserved by this user");
            }
            if (seat.getReservedUntil() == null || LocalDateTime.now().isAfter(seat.getReservedUntil())) {
                throw new HoldRejectedException("SEAT_RESERVATION_EXPIRED", "Seat " + seatId + " reservation has expired");
            }
            seats.add(seat);
        }

        Map<Integer, BigDecimal> sectionPrices = new HashMap<>();
        for (Seat seat : seats) {
            sectionPrices.computeIfAbsent(seat.getEventSectionId(), sectionId -> priceInEvent(sectionId, eventId));
        }
        return new HeldSeats(CURRENCY, seats.stream()
                .map(s -> new HeldSeat(s.getSeatId(), s.getEventSectionId(), sectionPrices.get(s.getEventSectionId())))
                .toList());
    }

    /** Todas las sillas deben ser del evento de la orden: si no, su dinero quedaría atribuido a otro evento. */
    private BigDecimal priceInEvent(Integer sectionId, Long eventId) {
        EventSection section = eventSectionRepository.findById(sectionId)
                .orElseThrow(() -> new HoldRejectedException("SEAT_NOT_IN_EVENT", "Seat section not found: " + sectionId));
        if (!section.getEventId().equals(Math.toIntExact(eventId))) {
            throw new HoldRejectedException("SEAT_NOT_IN_EVENT",
                    "Seat section " + sectionId + " does not belong to event " + eventId);
        }
        return section.getPrice();
    }
}
