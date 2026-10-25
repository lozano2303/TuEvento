package com.capysoft.tuevento.modules.event.application.usecase;

import com.capysoft.tuevento.modules.event.application.dto.response.OrganizerEarningsByEventResponse;
import com.capysoft.tuevento.modules.event.application.dto.response.OrganizerEarningsResponse;
import com.capysoft.tuevento.modules.payment.domain.repository.PaymentRepository;
import com.capysoft.tuevento.modules.seat.domain.model.EventSeatSummary;
import com.capysoft.tuevento.modules.seat.domain.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Retorna las ganancias del organizador autenticado.
 *
 * Flujo:
 *  1. Consulta los pagos APPROVED de los eventos del organizador, agrupados por evento
 *     (lo cobrado y las entradas vendidas).
 *  2. Consulta las sillas de sus eventos no cancelados: ganancia potencial y cortesías.
 *  3. Une ambos por evento: aparecen los eventos con ventas y los que tienen sillas.
 *
 * Lo cobrado nunca descuenta cortesías: una cortesía no genera pago. Solo reducen lo potencial.
 */
@Service
@RequiredArgsConstructor
public class GetOrganizerEarningsService {

    private static final String DEFAULT_CURRENCY = "COP";

    private final PaymentRepository paymentRepository;
    private final SeatRepository seatRepository;

    @Transactional(readOnly = true)
    public OrganizerEarningsResponse execute(Long organizerId) {
        // Ambas consultas agrupan por evento: una fila por eventId. El orden final lo fija el sort
        Map<Long, OrganizerEarningsByEventResponse> revenueByEvent = paymentRepository
                .findEarningsByOrganizer(organizerId).stream()
                .collect(Collectors.toMap(OrganizerEarningsByEventResponse::getEventId, Function.identity()));
        Map<Long, EventSeatSummary> seatsByEvent = seatRepository.summarizeSeatsByOrganizer(organizerId).stream()
                .collect(Collectors.toMap(EventSeatSummary::eventId, Function.identity()));

        List<OrganizerEarningsByEventResponse> byEvent = Stream
                .concat(revenueByEvent.keySet().stream(), seatsByEvent.keySet().stream())
                .distinct()
                .map(eventId -> merge(revenueByEvent.get(eventId), seatsByEvent.get(eventId)))
                .sorted(Comparator.comparing(OrganizerEarningsByEventResponse::getTotalRevenue).reversed()
                        .thenComparing(OrganizerEarningsByEventResponse::getPotentialRevenue, Comparator.reverseOrder()))
                .toList();

        String currency = revenueByEvent.values().stream().findFirst()
                .map(OrganizerEarningsByEventResponse::getCurrency)
                .orElse(DEFAULT_CURRENCY);

        return OrganizerEarningsResponse.builder()
                .totalEarnings(sum(byEvent, OrganizerEarningsByEventResponse::getTotalRevenue))
                .totalTicketsSold(byEvent.stream().mapToInt(OrganizerEarningsByEventResponse::getTicketsSold).sum())
                .currency(currency)
                .totalPotentialEarnings(sum(byEvent, OrganizerEarningsByEventResponse::getPotentialRevenue))
                .totalCourtesySeats(byEvent.stream().mapToLong(OrganizerEarningsByEventResponse::getCourtesySeats).sum())
                .earningsByEvent(byEvent)
                .build();
    }

    /** Une lo cobrado de un evento con el resumen de sus sillas; cualquiera de los dos puede faltar. */
    private static OrganizerEarningsByEventResponse merge(OrganizerEarningsByEventResponse revenue,
                                                          EventSeatSummary seats) {
        Optional<OrganizerEarningsByEventResponse> paid = Optional.ofNullable(revenue);
        Optional<EventSeatSummary> summary = Optional.ofNullable(seats);
        return OrganizerEarningsByEventResponse.builder()
                .eventId(paid.map(OrganizerEarningsByEventResponse::getEventId).orElseGet(() -> seats.eventId()))
                .eventName(paid.map(OrganizerEarningsByEventResponse::getEventName).orElseGet(() -> seats.eventName()))
                .ticketsSold(paid.map(OrganizerEarningsByEventResponse::getTicketsSold).orElse(0))
                .totalRevenue(paid.map(OrganizerEarningsByEventResponse::getTotalRevenue).orElse(BigDecimal.ZERO))
                .currency(paid.map(OrganizerEarningsByEventResponse::getCurrency).orElse(DEFAULT_CURRENCY))
                .potentialRevenue(summary.map(EventSeatSummary::potentialRevenue).orElse(BigDecimal.ZERO))
                .courtesySeats(summary.map(EventSeatSummary::courtesySeats).orElse(0L))
                .totalSeats(summary.map(EventSeatSummary::totalSeats).orElse(0L))
                .build();
    }

    private static BigDecimal sum(List<OrganizerEarningsByEventResponse> rows,
                                  Function<OrganizerEarningsByEventResponse, BigDecimal> field) {
        return rows.stream().map(field).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
