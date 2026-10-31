package com.capysoft.commerce.payment.application.usecase;

import com.capysoft.commerce.payment.application.dto.response.OrganizerEarningsByEventResponse;
import com.capysoft.commerce.payment.application.dto.response.OrganizerEarningsResponse;
import com.capysoft.commerce.payment.application.port.out.OrganizerSeatsPort;
import com.capysoft.commerce.payment.application.port.out.OrganizerSeatsPort.EventSeats;
import com.capysoft.commerce.payment.domain.repository.PaymentRepository;
import com.capysoft.commerce.ticket.application.port.out.EventLookupPort;
import com.capysoft.commerce.ticket.application.port.out.EventLookupPort.EventRef;
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
 *  1. Consulta los eventos del organizador y sus pagos APPROVED, agrupados por evento
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
    private final OrganizerSeatsPort organizerSeatsPort;
    private final EventLookupPort eventLookupPort;

    @Transactional(readOnly = true)
    public OrganizerEarningsResponse execute(Long organizerId) {
        Map<Long, EventRef> events = eventLookupPort.findByOwner(organizerId).stream()
                .collect(Collectors.toMap(EventRef::eventId, Function.identity()));
        // Ambas consultas agrupan por evento: una fila por eventId. El orden final lo fija el sort
        Map<Long, OrganizerEarningsByEventResponse> revenueByEvent = events.isEmpty() ? Map.of() : paymentRepository
                .findEarningsByEvents(events.keySet()).stream()
                .collect(Collectors.toMap(OrganizerEarningsByEventResponse::getEventId, Function.identity()));
        Map<Long, EventSeats> seatsByEvent = organizerSeatsPort.summarizeByOrganizer(organizerId).stream()
                .collect(Collectors.toMap(EventSeats::eventId, Function.identity()));

        List<OrganizerEarningsByEventResponse> byEvent = Stream
                .concat(revenueByEvent.keySet().stream(), seatsByEvent.keySet().stream())
                .distinct()
                .map(eventId -> merge(eventId, events.get(eventId), revenueByEvent.get(eventId), seatsByEvent.get(eventId)))
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
    private static OrganizerEarningsByEventResponse merge(Long eventId, EventRef event,
                                                          OrganizerEarningsByEventResponse revenue,
                                                          EventSeats seats) {
        Optional<OrganizerEarningsByEventResponse> paid = Optional.ofNullable(revenue);
        Optional<EventSeats> summary = Optional.ofNullable(seats);
        return OrganizerEarningsByEventResponse.builder()
                .eventId(eventId)
                .eventName(Optional.ofNullable(event).map(EventRef::name).orElseGet(() -> seats.eventName()))
                .ticketsSold(paid.map(OrganizerEarningsByEventResponse::getTicketsSold).orElse(0))
                .totalRevenue(paid.map(OrganizerEarningsByEventResponse::getTotalRevenue).orElse(BigDecimal.ZERO))
                .currency(paid.map(OrganizerEarningsByEventResponse::getCurrency).orElse(DEFAULT_CURRENCY))
                .potentialRevenue(summary.map(EventSeats::potentialRevenue).orElse(BigDecimal.ZERO))
                .courtesySeats(summary.map(EventSeats::courtesySeats).orElse(0L))
                .totalSeats(summary.map(EventSeats::totalSeats).orElse(0L))
                .build();
    }

    private static BigDecimal sum(List<OrganizerEarningsByEventResponse> rows,
                                  Function<OrganizerEarningsByEventResponse, BigDecimal> field) {
        return rows.stream().map(field).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
