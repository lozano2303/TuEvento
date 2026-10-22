package com.capysoft.tuevento.modules.ticket.application.usecase;

import com.capysoft.tuevento.modules.ticket.application.dto.response.OrderResponse;
import com.capysoft.tuevento.modules.ticket.application.dto.response.TicketResponse;
import com.capysoft.tuevento.modules.ticket.domain.model.Order;
import com.capysoft.tuevento.modules.ticket.domain.model.OrderStatus;
import com.capysoft.tuevento.modules.ticket.domain.model.Ticket;
import com.capysoft.tuevento.modules.ticket.domain.repository.OrderRepository;
import com.capysoft.tuevento.modules.ticket.domain.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                    return OrderResponse.fromDomainWithTickets(order, tickets);
                })
                .collect(Collectors.toList());
    }
}
