package com.capysoft.tuevento.modules.ticket.application.usecase;

import com.capysoft.tuevento.modules.ticket.application.dto.response.TicketResponse;
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
 * Use case para obtener los tickets de una orden.
 */
@Service
@RequiredArgsConstructor
public class GetOrderTicketsUseCaseImpl {
    
    private final OrderRepository orderRepository;
    private final TicketRepository ticketRepository;
    
    @Transactional(readOnly = true)
    public List<TicketResponse> execute(Long orderId) {
        // Validar que la orden exista
        orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        List<Ticket> tickets = ticketRepository.findByOrderId(orderId);
        
        return tickets.stream()
            .map(TicketResponse::fromDomain)
            .collect(Collectors.toList());
    }
}
