package com.capysoft.tuevento.modules.ticket.application.usecase;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.capysoft.tuevento.modules.seat.application.dto.request.UpdateSeatStatusRequest;
import com.capysoft.tuevento.modules.seat.application.port.in.SeatUseCase;
import com.capysoft.tuevento.modules.seat.domain.model.SeatStatus;
import com.capysoft.tuevento.modules.ticket.application.port.in.FailOrderPaymentUseCase;
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
 * Implementación del puerto de entrada para marcar orden como pago fallido.
 * Será invocado por el módulo payment cuando el pago falle.
 */
@Service
@RequiredArgsConstructor
public class FailOrderPaymentUseCaseImpl implements FailOrderPaymentUseCase {
    
    private final OrderRepository orderRepository;
    private final TicketRepository ticketRepository;
    private final SeatTicketRepository seatTicketRepository;
    private final TicketLogRepository ticketLogRepository;
    private final SeatUseCase seatUseCase;
    
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
        
        // Liberar las sillas marcándolas como AVAILABLE
        for (Ticket ticket : tickets) {
            List<SeatTicket> seatTickets = seatTicketRepository.findByTicketId(ticket.getTicketId());
            for (SeatTicket seatTicket : seatTickets) {
                UpdateSeatStatusRequest seatStatusRequest = UpdateSeatStatusRequest.builder()
                    .newStatus(SeatStatus.AVAILABLE)
                    .reason("Payment failed: " + reason)
                    .build();
                seatUseCase.updateSeatStatus(seatTicket.getSeatId(), seatStatusRequest, null);
            }
        }
    }
}
