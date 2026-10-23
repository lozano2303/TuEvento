package com.capysoft.tuevento.modules.ticket.application.usecase;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.capysoft.tuevento.modules.seat.application.dto.request.UpdateSeatStatusRequest;
import com.capysoft.tuevento.modules.seat.application.port.in.SeatUseCase;
import com.capysoft.tuevento.modules.seat.domain.model.SeatStatus;
import com.capysoft.tuevento.modules.ticket.application.port.in.ConfirmOrderPaymentUseCase;
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
 * Implementación del puerto de entrada para confirmar pago de orden.
 * Será invocado por el módulo payment.
 */
@Service
@RequiredArgsConstructor
public class ConfirmOrderPaymentUseCaseImpl implements ConfirmOrderPaymentUseCase {
    
    private final OrderRepository orderRepository;
    private final TicketRepository ticketRepository;
    private final TicketLogRepository ticketLogRepository;
    private final SeatTicketRepository seatTicketRepository;
    private final SeatUseCase seatUseCase;
    
    @Override
    @Transactional
    public void confirmOrderPayment(Long orderId, String providerPaymentId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        // Transicionar orden a PAID
        order.markAsPaid();
        orderRepository.save(order);
        
        // Transicionar todos los tickets a PAID
        List<Ticket> tickets = ticketRepository.findByOrderId(orderId);
        
        for (Ticket ticket : tickets) {
            TicketStatus oldStatus = ticket.getStatus();
            ticket.markAsPaid();
            ticketRepository.save(ticket);
            
            // Log de auditoría
            TicketLog log = TicketLog.builder()
                .ticketId(ticket.getTicketId())
                .oldStatus(oldStatus)
                .newStatus(TicketStatus.PAID)
                .changedAt(LocalDateTime.now())
                .changedBy("payment-module")
                .reason("Payment confirmed: " + providerPaymentId)
                .build();
            ticketLogRepository.save(log);
        }
        
        // Marcar sillas como SOLD
        for (Ticket ticket : tickets) {
            List<SeatTicket> seatTickets = seatTicketRepository.findByTicketId(ticket.getTicketId());
            for (SeatTicket seatTicket : seatTickets) {
                UpdateSeatStatusRequest seatStatusRequest = UpdateSeatStatusRequest.builder()
                    .newStatus(SeatStatus.SOLD)
                    .reason("Payment confirmed: " + providerPaymentId)
                    .build();
                seatUseCase.updateSeatStatus(seatTicket.getSeatId(), seatStatusRequest, null);
            }
        }
    }
}
