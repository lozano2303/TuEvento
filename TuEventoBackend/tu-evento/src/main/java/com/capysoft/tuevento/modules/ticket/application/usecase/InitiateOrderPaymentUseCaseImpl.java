package com.capysoft.tuevento.modules.ticket.application.usecase;

import com.capysoft.tuevento.modules.ticket.application.port.in.InitiateOrderPaymentUseCase;
import com.capysoft.tuevento.modules.ticket.domain.model.*;
import com.capysoft.tuevento.modules.ticket.domain.repository.OrderRepository;
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
 * Implementación del puerto de entrada para iniciar pago de orden.
 * Transiciona Order de DRAFT a PAYMENT_PENDING.
 */
@Service
@RequiredArgsConstructor
public class InitiateOrderPaymentUseCaseImpl implements InitiateOrderPaymentUseCase {
    
    private final OrderRepository orderRepository;
    private final TicketRepository ticketRepository;
    private final TicketLogRepository ticketLogRepository;
    
    @Override
    @Transactional
    public void initiateOrderPayment(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        // Transicionar orden a PAYMENT_PENDING (validará que esté en DRAFT)
        order.markAsPaymentPending();
        orderRepository.save(order);
        
        // Transicionar todos los tickets a PROCESSING
        List<Ticket> tickets = ticketRepository.findByOrderId(orderId);
        String changedBy = getCurrentUsername();
        
        for (Ticket ticket : tickets) {
            TicketStatus oldStatus = ticket.getStatus();
            // No hay método markAsProcessing en Ticket, actualizo directamente el builder
            Ticket updatedTicket = Ticket.builder()
                .ticketId(ticket.getTicketId())
                .eventId(ticket.getEventId())
                .userId(ticket.getUserId())
                .orderId(ticket.getOrderId())
                .code(ticket.getCode())
                .qrCode(ticket.getQrCode())
                .status(TicketStatus.PROCESSING)
                .expirationDate(ticket.getExpirationDate())
                .totalPrice(ticket.getTotalPrice())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .createdBy(ticket.getCreatedBy())
                .updatedBy(ticket.getUpdatedBy())
                .build();
            
            ticketRepository.save(updatedTicket);
            
            // Log de auditoría
            TicketLog log = TicketLog.builder()
                .ticketId(ticket.getTicketId())
                .oldStatus(oldStatus)
                .newStatus(TicketStatus.PROCESSING)
                .changedAt(LocalDateTime.now())
                .changedBy(changedBy)
                .reason("Payment initiated")
                .build();
            ticketLogRepository.save(log);
        }
    }
    
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : "system";
    }
}
