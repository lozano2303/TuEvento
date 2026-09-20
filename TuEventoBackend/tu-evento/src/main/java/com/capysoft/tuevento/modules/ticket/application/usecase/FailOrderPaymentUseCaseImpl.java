package com.capysoft.tuevento.modules.ticket.application.usecase;

import com.capysoft.tuevento.modules.seat.domain.model.Seat;
import com.capysoft.tuevento.modules.seat.domain.repository.SeatRepository;
import com.capysoft.tuevento.modules.ticket.application.port.in.FailOrderPaymentUseCase;
import com.capysoft.tuevento.modules.ticket.domain.model.*;
import com.capysoft.tuevento.modules.ticket.domain.repository.OrderRepository;
import com.capysoft.tuevento.modules.ticket.domain.repository.SeatTicketRepository;
import com.capysoft.tuevento.modules.ticket.domain.repository.TicketLogRepository;
import com.capysoft.tuevento.modules.ticket.domain.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementación del puerto de entrada para marcar orden como pago fallido.
 * Será invocado por el módulo payment cuando el pago falle.
 */
@Service
@RequiredArgsConstructor
public class FailOrderPaymentUseCaseImpl implements FailOrderPaymentUseCase {
    
    private final OrderRepository orderRepository;
    private final TicketRepository ticketRepository;
    private final SeatTicketRepository seatTicketRepository;
    private final SeatRepository seatRepository;
    private final TicketLogRepository ticketLogRepository;
    
    @Override
    @Transactional
    public void failOrderPayment(Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        // La orden puede permanecer en su estado actual (PAYMENT_PENDING)
        // para permitir reintento, o se puede cancelar
        // Por ahora la cancelamos para liberar las sillas
        order.cancel();
        orderRepository.save(order);
        
        // Cancelar todos los tickets
        List<Ticket> tickets = ticketRepository.findByOrderId(orderId);
        
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
                .changedBy("payment-module")
                .reason("Payment failed: " + reason)
                .build();
            ticketLogRepository.save(log);
        }
        
        // Liberar las sillas
        for (Ticket ticket : tickets) {
            List<SeatTicket> seatTickets = seatTicketRepository.findByTicketId(ticket.getTicketId());
            for (SeatTicket seatTicket : seatTickets) {
                Seat seat = seatRepository.findById(seatTicket.getSeatId())
                    .orElseThrow(() -> new IllegalStateException("Seat not found: " + seatTicket.getSeatId()));
                
                // Liberar silla
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
}
