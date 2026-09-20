package com.capysoft.tuevento.modules.ticket.application.usecase;

import com.capysoft.tuevento.modules.seat.domain.model.Seat;
import com.capysoft.tuevento.modules.seat.domain.repository.SeatRepository;
import com.capysoft.tuevento.modules.ticket.domain.model.*;
import com.capysoft.tuevento.modules.ticket.domain.repository.OrderRepository;
import com.capysoft.tuevento.modules.ticket.domain.repository.SeatTicketRepository;
import com.capysoft.tuevento.modules.ticket.domain.repository.TicketLogRepository;
import com.capysoft.tuevento.modules.ticket.domain.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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
    private final SeatRepository seatRepository;
    private final TicketLogRepository ticketLogRepository;
    
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
        
        // Liberar las sillas
        for (Ticket ticket : tickets) {
            List<SeatTicket> seatTickets = seatTicketRepository.findByTicketId(ticket.getTicketId());
            for (SeatTicket seatTicket : seatTickets) {
                Seat seat = seatRepository.findById(seatTicket.getSeatId())
                    .orElseThrow(() -> new IllegalStateException("Seat not found: " + seatTicket.getSeatId()));
                
                // Liberar silla (remover reserva)
                Seat updatedSeat = Seat.builder()
                    .seatId(seat.getSeatId())
                    .seatBlockId(seat.getSeatBlockId())
                    .eventSectionId(seat.getEventSectionId())
                    .code(seat.getCode())
                    .row(seat.getRow())
                    .position(seat.getPosition())
                    .type(seat.getType())
                    .status(seat.getStatus())
                    .reservedBy(null)
                    .reservedUntil(null)
                    .build();
                
                seatRepository.save(updatedSeat);
            }
        }
    }
    
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : "system";
    }
}
