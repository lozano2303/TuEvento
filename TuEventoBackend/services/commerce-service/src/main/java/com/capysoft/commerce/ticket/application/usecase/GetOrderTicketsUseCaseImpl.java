package com.capysoft.commerce.ticket.application.usecase;

import com.capysoft.commerce.ticket.application.dto.response.TicketResponse;
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
 * Use case para obtener los tickets de una orden.
 */
@Service
@RequiredArgsConstructor
public class GetOrderTicketsUseCaseImpl {
    
    private final OrderRepository orderRepository;
    private final TicketRepository ticketRepository;
    
    @Transactional(readOnly = true)
    public List<TicketResponse> execute(Long orderId, Long requesterId) {
        // Validar que la orden exista y sea del solicitante
        boolean owned = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId))
            .isOwnedBy(requesterId);
        if (!owned) {
            throw new AccessDeniedException("You do not have access to this order");
        }
        
        List<Ticket> tickets = ticketRepository.findByOrderId(orderId);
        
        return tickets.stream()
            .map(TicketResponse::fromDomain)
            .collect(Collectors.toList());
    }
}
