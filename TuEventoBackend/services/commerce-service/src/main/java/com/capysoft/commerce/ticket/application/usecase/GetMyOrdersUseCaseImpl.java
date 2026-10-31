package com.capysoft.commerce.ticket.application.usecase;

import com.capysoft.commerce.ticket.application.port.out.EventLookupPort;
import com.capysoft.commerce.ticket.application.port.out.EventLookupPort.EventRef;
import com.capysoft.commerce.ticket.application.dto.response.OrderResponse;
import com.capysoft.commerce.ticket.application.dto.response.TicketResponse;
import com.capysoft.commerce.ticket.domain.model.Order;
import com.capysoft.commerce.ticket.domain.model.OrderStatus;
import com.capysoft.commerce.ticket.domain.repository.OrderRepository;
import com.capysoft.commerce.ticket.domain.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Lista las órdenes del usuario autenticado, con sus tickets, opcionalmente
 * filtradas por estado (p. ej. PAID). Base de la pantalla "mis entradas".
 */
@Service
@RequiredArgsConstructor
public class GetMyOrdersUseCaseImpl {

    private final OrderRepository orderRepository;
    private final TicketRepository ticketRepository;
    private final EventLookupPort eventLookupPort;

    @Transactional(readOnly = true)
    public List<OrderResponse> execute(Long userId, OrderStatus statusFilter) {
        List<Order> orders = orderRepository.findByUserId(userId);

        return orders.stream()
                .filter(o -> statusFilter == null || o.getStatus() == statusFilter)
                .map(order -> {
                    List<TicketResponse> tickets = ticketRepository
                            .findByOrderId(order.getOrderId()).stream()
                            .map(TicketResponse::fromDomain)
                            .collect(Collectors.toList());

                    EventRef event = eventLookupPort.find(order.getEventId()).orElse(null);
                    String eventName   = event != null ? event.name() : null;
                    String eventStatus = event != null ? event.status() : null;

                    return OrderResponse.fromDomainWithTickets(order, tickets, eventName, eventStatus);
                })
                // Prioridad de orden: primero eventos PUBLISHED (entradas usables),
                // y dentro de cada grupo, la orden más nueva primero.
                .sorted(Comparator
                        .comparing((OrderResponse o) -> "PUBLISHED".equals(o.getEventStatus()) ? 0 : 1)
                        .thenComparing(OrderResponse::getCreatedAt,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }
}
