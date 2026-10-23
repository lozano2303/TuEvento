package com.capysoft.tuevento.modules.ticket.application.usecase;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.capysoft.tuevento.modules.seat.application.dto.request.UpdateSeatStatusRequest;
import com.capysoft.tuevento.modules.seat.application.port.in.SeatUseCase;
import com.capysoft.tuevento.modules.seat.domain.model.SeatStatus;
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
 * Solo permitido en estados DRAFT o PAYMENT_PENDING.
 */
@Service
@RequiredArgsConstructor
public class CancelOrderUseCaseImpl {
    
    private final OrderRepository orderRepository;
    private final TicketRepository ticketRepository;
    private final SeatTicketRepository seatTicketRepository;
    private final TicketLogRepository ticketLogRepository;
    private final SeatUseCase seatUseCase;
    
    @Transactional
    public void execute(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        
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
        for (Ticket ticket : tickets) {
            List<SeatTicket> seatTickets = seatTicketRepository.findByTicketId(ticket.getTicketId());
            for (SeatTicket seatTicket : seatTickets) {
                UpdateSeatStatusRequest seatStatusRequest = UpdateSeatStatusRequest.builder()
                    .newStatus(SeatStatus.AVAILABLE)
                    .reason("Order cancelled")
                    .build();
                
                Integer userId = null;
                try {
                    userId = Integer.parseInt(getCurrentUsername());
                } catch (NumberFormatException e) {
                    // changedBy será null si no es un userId válido
                }
                
                seatUseCase.updateSeatStatus(seatTicket.getSeatId(), seatStatusRequest, userId);
            }
        }
    }
    
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : "system";
    }
}
