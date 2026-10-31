package com.capysoft.commerce.ticket.application.usecase;

import com.capysoft.commerce.ticket.application.port.out.EventLookupPort;
import com.capysoft.commerce.ticket.application.port.out.EventLookupPort.EventRef;
import com.capysoft.commerce.ticket.application.dto.response.TicketQrResponse;
import com.capysoft.commerce.ticket.domain.model.Order;
import com.capysoft.commerce.ticket.domain.model.OrderNotFoundException;
import com.capysoft.commerce.ticket.domain.model.Ticket;
import com.capysoft.commerce.ticket.domain.model.TicketStatus;
import com.capysoft.commerce.ticket.domain.port.QrNonceStore;
import com.capysoft.commerce.ticket.domain.port.QrTokenPort;
import com.capysoft.commerce.ticket.domain.port.QrTokenPort.QrScope;
import com.capysoft.commerce.ticket.domain.repository.OrderRepository;
import com.capysoft.commerce.ticket.domain.repository.TicketRepository;
import com.capysoft.common.domain.exception.BusinessException;
import com.capysoft.common.domain.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Genera el QR de una orden completa.
 *
 * Al escanearse marcará como USED todos los tickets de la orden que aún estén
 * PAID (las "sillas faltantes"). Si algunos ya fueron escaneados de forma
 * individual, no pasa nada: el QR de orden solo necesita que quede al menos un
 * ticket PAID para ser útil.
 */
@Service
@RequiredArgsConstructor
public class GenerateOrderQrUseCaseImpl {

    private final OrderRepository orderRepository;
    private final TicketRepository ticketRepository;
    private final EventLookupPort eventLookupPort;
    private final QrTokenPort qrTokenPort;
    private final QrNonceStore qrNonceStore;

    @Transactional(readOnly = true)
    public TicketQrResponse execute(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!order.getUserId().equals(userId)) {
            throw new AccessDeniedException("You do not own this order");
        }

        // El evento debe seguir publicado: cancelado/finalizado → no se puede validar.
        EventRef event = eventLookupPort.find(order.getEventId())
                .orElseThrow(() -> new NotFoundException("EVENT_NOT_FOUND",
                        "Event not found for this order"));
        if (!event.isPublished()) {
            throw new BusinessException("EVENT_NOT_ACTIVE",
                    "The event is not active (status: " + event.status() + ")");
        }

        List<Ticket> tickets = ticketRepository.findByOrderId(orderId);
        boolean hasRemaining = tickets.stream()
                .anyMatch(t -> t.getStatus() == TicketStatus.PAID);

        if (!hasRemaining) {
            throw new BusinessException("ORDER_NO_VALIDATABLE_TICKETS",
                    "This order has no remaining tickets to validate");
        }

        long ttl = qrTokenPort.getExpirationSeconds();
        String nonce = UUID.randomUUID().toString();

        // Reemplaza el nonce vigente → invalida cualquier QR de orden anterior.
        qrNonceStore.store(QrScope.ORDER, orderId, nonce, ttl);

        String token = qrTokenPort.generate(
                QrScope.ORDER, orderId, order.getEventId(), userId, nonce);

        return TicketQrResponse.builder()
                .scope(QrScope.ORDER.name())
                .targetId(orderId)
                .qrToken(token)
                .expiresAt(LocalDateTime.now().plusSeconds(ttl))
                .expiresInSeconds(ttl)
                .build();
    }
}
