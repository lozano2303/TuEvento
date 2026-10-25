package com.capysoft.tuevento.modules.ticket.application.usecase;

import com.capysoft.tuevento.modules.ticket.application.dto.response.RefundableTicketsSummary;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementación del puerto de entrada para confirmar el reembolso de una orden.
 *
 * Solo los tickets PAID pasan a REFUNDED: un ticket USED ya se consumió y nunca se
 * reembolsa. La orden pasa a REFUNDED únicamente si todos sus tickets se
 * reembolsaron; si alguno se usó, la orden sigue en PAID y cada ticket refleja su
 * estado real (así sigue apareciendo en "Mis tickets").
 *
 * Las sillas NO se liberan — decisión de negocio: ya fueron usadas o el evento
 * pasó/fue cancelado, y no vuelven a estar disponibles para venta en este flujo.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfirmRefundUseCaseImpl implements ConfirmRefundUseCase {

    private final OrderRepository orderRepository;
    private final TicketRepository ticketRepository;
    private final TicketLogRepository ticketLogRepository;

    @Override
    @Transactional(readOnly = true)
    public RefundableTicketsSummary getRefundableTickets(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));

        List<Ticket> tickets = activeTickets(orderId);
        List<Ticket> refundable = tickets.stream()
            .filter(t -> t.getStatus() == TicketStatus.PAID)
            .toList();

        BigDecimal amount = refundable.stream()
            .map(t -> t.getTotalPrice() != null ? t.getTotalPrice().getAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        String currency = order.getTotalAmount() != null ? order.getTotalAmount().getCurrency() : null;

        return new RefundableTicketsSummary(amount, currency, refundable.size(), tickets.size());
    }

    @Override
    @Transactional
    public void confirmRefund(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));

        List<Ticket> tickets = activeTickets(orderId);
        long refunded = 0;
        long used = 0;

        // 1. Solo los tickets PAID se reembolsan; los USED se conservan
        for (Ticket ticket : tickets) {
            if (ticket.getStatus() == TicketStatus.USED) {
                used++;
                continue;
            }
            if (ticket.getStatus() != TicketStatus.PAID) {
                continue;
            }
            TicketStatus oldStatus = ticket.getStatus();
            ticket.markAsRefunded();
            ticketRepository.save(ticket);
            refunded++;

            ticketLogRepository.save(TicketLog.builder()
                .ticketId(ticket.getTicketId())
                .oldStatus(oldStatus)
                .newStatus(TicketStatus.REFUNDED)
                .changedAt(LocalDateTime.now())
                .changedBy("payment-module")
                .reason("Refund confirmed: unused ticket")
                .build());
        }

        // 2. La orden solo pasa a REFUNDED si ningún ticket se usó
        if (used == 0 && refunded > 0) {
            order.refund(); // valida PAID → REFUNDED
            orderRepository.save(order);
        } else if (used > 0) {
            log.info("Partial refund on order {}: {} unused ticket(s) refunded, {} used ticket(s) kept; order stays {}",
                orderId, refunded, used, order.getStatus());
        }

        // Nota: las sillas NO se liberan en este flujo; las vendidas permanecen en SOLD.
    }

    /** Tickets de la orden que cuentan para el reembolso (los cancelados no forman parte de la venta). */
    private List<Ticket> activeTickets(Long orderId) {
        return ticketRepository.findByOrderId(orderId).stream()
            .filter(t -> t.getStatus() != TicketStatus.CANCELLED)
            .toList();
    }
}
