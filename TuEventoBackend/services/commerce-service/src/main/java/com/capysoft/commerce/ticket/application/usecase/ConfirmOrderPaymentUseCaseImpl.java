package com.capysoft.commerce.ticket.application.usecase;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.capysoft.commerce.ticket.application.port.out.CatalogSeatsPort;
import com.capysoft.commerce.ticket.application.port.in.ConfirmOrderPaymentUseCase;
import com.capysoft.commerce.ticket.domain.model.Order;
import com.capysoft.commerce.ticket.domain.model.OrderNotFoundException;
import com.capysoft.commerce.ticket.domain.model.SeatTicket;
import com.capysoft.commerce.ticket.domain.model.Ticket;
import com.capysoft.commerce.ticket.domain.model.TicketLog;
import com.capysoft.commerce.ticket.domain.model.TicketStatus;
import com.capysoft.commerce.ticket.domain.repository.OrderRepository;
import com.capysoft.commerce.ticket.domain.repository.SeatTicketRepository;
import com.capysoft.commerce.ticket.domain.repository.TicketLogRepository;
import com.capysoft.commerce.ticket.domain.repository.TicketRepository;

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
    private final CatalogSeatsPort catalogSeatsPort;
    
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
        
        // Marcar sillas como SOLD a nombre del comprador: si alguna la tomó otra persona mientras
        // tanto, falla y se deshace toda la confirmación (no se entrega dos veces la misma silla)
        catalogSeatsPort.sell(order.getOrderId(), order.getUserId(), seatIdsOf(tickets),
                "Payment confirmed: " + providerPaymentId);
    }

    private List<Integer> seatIdsOf(List<Ticket> tickets) {
        return tickets.stream()
                .flatMap(ticket -> seatTicketRepository.findByTicketId(ticket.getTicketId()).stream())
                .map(SeatTicket::getSeatId)
                .toList();
    }
}
