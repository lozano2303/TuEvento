package com.capysoft.tuevento.modules.ticket.application.usecase;

import com.capysoft.tuevento.modules.ticket.application.port.in.ConfirmRefundUseCase;
import com.capysoft.tuevento.modules.ticket.domain.model.Order;
import com.capysoft.tuevento.modules.ticket.domain.model.OrderNotFoundException;
import com.capysoft.tuevento.modules.ticket.domain.model.Ticket;
import com.capysoft.tuevento.modules.ticket.domain.model.TicketLog;
import com.capysoft.tuevento.modules.ticket.domain.model.TicketStatus;
import com.capysoft.tuevento.modules.ticket.domain.repository.OrderRepository;
import com.capysoft.tuevento.modules.ticket.domain.repository.TicketLogRepository;
import com.capysoft.tuevento.modules.ticket.domain.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementación del puerto de entrada para confirmar el reembolso de una orden.
 * Invocado por el módulo payment cuando llega el webhook payment.refunded.
 *
 * Transiciona Order: PAID → REFUNDED y cada Ticket a REFUNDED.
 * Las sillas NO se liberan — decisión de negocio: ya fueron usadas o el evento
 * pasó/fue cancelado, y no vuelven a estar disponibles para venta en este flujo.
 */
@Service
@RequiredArgsConstructor
public class ConfirmRefundUseCaseImpl implements ConfirmRefundUseCase {

    private final OrderRepository orderRepository;
    private final TicketRepository ticketRepository;
    private final TicketLogRepository ticketLogRepository;

    @Override
    @Transactional
    public void confirmRefund(Long orderId) {
        // 1. Cargar y transicionar la orden PAID → REFUNDED
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));

        order.refund(); // valida PAID → REFUNDED, lanza InvalidOrderStatusTransitionException si inválido
        orderRepository.save(order);

        // 2. Transicionar todos los tickets a REFUNDED + log de auditoría
        List<Ticket> tickets = ticketRepository.findByOrderId(orderId);

        for (Ticket ticket : tickets) {
            TicketStatus oldStatus = ticket.getStatus();
            ticket.markAsRefunded();
            ticketRepository.save(ticket);

            TicketLog ticketLog = TicketLog.builder()
                .ticketId(ticket.getTicketId())
                .oldStatus(oldStatus)
                .newStatus(TicketStatus.REFUNDED)
                .changedAt(LocalDateTime.now())
                .changedBy("payment-module")
                .reason("Refund confirmed via webhook payment.refunded")
                .build();
            ticketLogRepository.save(ticketLog);
        }

        // Nota: las sillas NO se liberan en este flujo.
        // Las sillas ya vendidas permanecen en estado SOLD.
    }
}
