package com.capysoft.tuevento.modules.ticket.application.usecase;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.capysoft.tuevento.modules.ticket.application.port.out.CatalogSeatsPort;
import com.capysoft.tuevento.modules.ticket.domain.model.Order;
import com.capysoft.tuevento.modules.ticket.domain.model.OrderNotFoundException;
import com.capysoft.tuevento.modules.ticket.domain.model.SeatTicket;
import com.capysoft.tuevento.modules.ticket.domain.model.Ticket;
import com.capysoft.tuevento.modules.ticket.domain.model.TicketLog;
import com.capysoft.tuevento.modules.ticket.domain.model.TicketStatus;
import com.capysoft.tuevento.modules.ticket.domain.repository.OrderRepository;
import com.capysoft.tuevento.modules.ticket.domain.repository.SeatTicketRepository;
import com.capysoft.tuevento.modules.ticket.domain.repository.TicketLogRepository;
import com.capysoft.tuevento.modules.ticket.domain.repository.TicketRepository;

import lombok.RequiredArgsConstructor;

/**
 * Use case para cancelar una orden.
 * Solo permitido en estados DRAFT o PAYMENT_PENDING y solo por el comprador.
 */
@Service
@RequiredArgsConstructor
public class CancelOrderUseCaseImpl {
    
    private final OrderRepository orderRepository;
    private final TicketRepository ticketRepository;
    private final SeatTicketRepository seatTicketRepository;
    private final TicketLogRepository ticketLogRepository;
    private final CatalogSeatsPort catalogSeatsPort;
    
    @Transactional
    public void execute(Long orderId, Long requesterId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        if (!order.isOwnedBy(requesterId)) {
            throw new AccessDeniedException("You cannot cancel this order");
        }
        
        // Transicionar orden a CANCELLED (validará estados permitidos)
        order.cancel();
        orderRepository.save(order);
        
        // Cancelar todos los tickets
        List<Ticket> tickets = ticketRepository.findByOrderId(orderId);
        String changedBy = getCurrentUsername();
        
        for (Ticket ticket : tickets) {
            TicketStatus oldStatus = ticket.getStatus();
            ticket.cancel();
            ticketRepository.save(ticket);
            
            // Log de auditoría
            TicketLog log = TicketLog.builder()
                .ticketId(ticket.getTicketId())
                .oldStatus(oldStatus)
                .newStatus(TicketStatus.CANCELLED)
                .changedAt(LocalDateTime.now())
                .changedBy(changedBy)
                .reason("Order cancelled")
                .build();
            ticketLogRepository.save(log);
        }
        
        // Liberar las sillas marcándolas como AVAILABLE
        List<Integer> seatIds = tickets.stream()
            .flatMap(ticket -> seatTicketRepository.findByTicketId(ticket.getTicketId()).stream())
            .map(SeatTicket::getSeatId)
            .toList();
        Integer changedByUserId = null;
        try {
            changedByUserId = Integer.parseInt(getCurrentUsername());
        } catch (NumberFormatException e) {
            // changedBy será null si no es un userId válido
        }
        catalogSeatsPort.release(order.getOrderId(), seatIds, changedByUserId, "Order cancelled");
    }
    
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : "system";
    }
}
