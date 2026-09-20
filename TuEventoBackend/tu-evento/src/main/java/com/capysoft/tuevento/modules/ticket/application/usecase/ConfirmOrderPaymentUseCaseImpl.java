package com.capysoft.tuevento.modules.ticket.application.usecase;

import com.capysoft.tuevento.modules.ticket.application.port.in.ConfirmOrderPaymentUseCase;
import com.capysoft.tuevento.modules.ticket.domain.model.*;
import com.capysoft.tuevento.modules.ticket.domain.repository.OrderRepository;
import com.capysoft.tuevento.modules.ticket.domain.repository.TicketLogRepository;
import com.capysoft.tuevento.modules.ticket.domain.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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
        
        // Las sillas ya estaban reservadas desde CreateOrderWithTicketsUseCase
        // Ahora quedan definitivamente asignadas (no se liberan)
    }
}
