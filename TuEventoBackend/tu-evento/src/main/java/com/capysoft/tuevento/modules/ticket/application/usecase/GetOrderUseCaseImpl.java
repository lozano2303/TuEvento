package com.capysoft.tuevento.modules.ticket.application.usecase;

import com.capysoft.tuevento.modules.ticket.application.dto.response.OrderResponse;
import com.capysoft.tuevento.modules.ticket.application.dto.response.TicketResponse;
import com.capysoft.tuevento.modules.ticket.domain.model.Order;
import com.capysoft.tuevento.modules.ticket.domain.model.OrderNotFoundException;
import com.capysoft.tuevento.modules.ticket.domain.model.Ticket;
import com.capysoft.tuevento.modules.ticket.domain.repository.OrderRepository;
import com.capysoft.tuevento.modules.ticket.domain.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
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
    public OrderResponse execute(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        List<Ticket> tickets = ticketRepository.findByOrderId(orderId);
        List<TicketResponse> ticketResponses = tickets.stream()
            .map(TicketResponse::fromDomain)
            .collect(Collectors.toList());
        
        return OrderResponse.fromDomainWithTickets(order, ticketResponses);
    }
}
