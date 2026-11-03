package com.capysoft.commerce.ticket.application.usecase;

import com.capysoft.commerce.ticket.application.dto.response.OrderResponse;
import com.capysoft.commerce.ticket.application.dto.response.TicketResponse;
import com.capysoft.commerce.ticket.domain.model.Order;
import com.capysoft.commerce.ticket.domain.model.OrderNotFoundException;
import com.capysoft.commerce.ticket.domain.model.Ticket;
import com.capysoft.commerce.ticket.domain.repository.OrderRepository;
import com.capysoft.commerce.ticket.domain.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Use case para obtener una orden por ID.
 */
@Service
@RequiredArgsConstructor
public class GetOrderUseCaseImpl {
    
    private final OrderRepository orderRepository;
    private final TicketRepository ticketRepository;
    
    @Transactional(readOnly = true)
    public OrderResponse execute(Long orderId, Long requesterId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        if (!order.isOwnedBy(requesterId)) {
            throw new AccessDeniedException("You do not have access to this order");
        }
        
        List<Ticket> tickets = ticketRepository.findByOrderId(orderId);
        List<TicketResponse> ticketResponses = tickets.stream()
            .map(TicketResponse::fromDomain)
            .collect(Collectors.toList());
        
        return OrderResponse.fromDomainWithTickets(order, ticketResponses);
    }
}
