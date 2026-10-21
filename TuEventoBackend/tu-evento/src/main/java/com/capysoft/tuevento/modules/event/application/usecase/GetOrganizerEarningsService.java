package com.capysoft.tuevento.modules.event.application.usecase;

import com.capysoft.tuevento.modules.event.application.dto.response.OrganizerEarningsByEventResponse;
import com.capysoft.tuevento.modules.event.application.dto.response.OrganizerEarningsResponse;
import com.capysoft.tuevento.modules.payment.domain.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Retorna las ganancias del organizador autenticado.
 *
 * Flujo:
 *  1. Consulta los pagos APPROVED de los eventos del organizador, agrupados por evento.
 *  2. Calcula el total acumulado y total de tickets vendidos.
 *  3. Retorna OrganizerEarningsResponse con el desglose por evento.
 */
@Service
@RequiredArgsConstructor
public class GetOrganizerEarningsService {

    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public OrganizerEarningsResponse execute(Long organizerId) {
        List<OrganizerEarningsByEventResponse> byEvent =
                paymentRepository.findEarningsByOrganizer(organizerId);

        BigDecimal totalEarnings = byEvent.stream()
                .map(OrganizerEarningsByEventResponse::getTotalRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalTickets = byEvent.stream()
                .mapToInt(OrganizerEarningsByEventResponse::getTicketsSold)
                .sum();

        String currency = byEvent.isEmpty() ? "COP" : byEvent.get(0).getCurrency();

        return OrganizerEarningsResponse.builder()
                .totalEarnings(totalEarnings)
                .totalTicketsSold(totalTickets)
                .currency(currency)
                .earningsByEvent(byEvent)
                .build();
    }
}
