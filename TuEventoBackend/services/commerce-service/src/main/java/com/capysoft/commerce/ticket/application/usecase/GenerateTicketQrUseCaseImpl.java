package com.capysoft.commerce.ticket.application.usecase;

import com.capysoft.commerce.ticket.application.port.out.EventLookupPort;
import com.capysoft.commerce.ticket.application.port.out.EventLookupPort.EventRef;
import com.capysoft.commerce.ticket.application.dto.response.TicketQrResponse;
import com.capysoft.commerce.ticket.domain.model.Ticket;
import com.capysoft.commerce.ticket.domain.model.TicketNotFoundException;
import com.capysoft.commerce.ticket.domain.model.TicketStatus;
import com.capysoft.commerce.ticket.domain.port.QrNonceStore;
import com.capysoft.commerce.ticket.domain.port.QrTokenPort;
import com.capysoft.commerce.ticket.domain.port.QrTokenPort.QrScope;
import com.capysoft.commerce.ticket.domain.repository.TicketRepository;
import com.capysoft.common.domain.exception.BusinessException;
import com.capysoft.common.domain.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Genera el QR de un ticket individual.
 *
 * Reglas:
 *   - El ticket debe existir y pertenecer al usuario autenticado (anti-reventa:
 *     solo el comprador puede mostrar el QR).
 *   - El ticket debe estar PAID (ya pagado y no usado/cancelado).
 *   - Se crea un nonce nuevo que reemplaza el anterior en el store → el QR
 *     generado previamente queda invalidado.
 */
@Service
@RequiredArgsConstructor
public class GenerateTicketQrUseCaseImpl {

    private final TicketRepository ticketRepository;
    private final EventLookupPort eventLookupPort;
    private final QrTokenPort qrTokenPort;
    private final QrNonceStore qrNonceStore;

    @Transactional(readOnly = true)
    public TicketQrResponse execute(Long ticketId, Long userId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

        if (!ticket.getUserId().equals(userId)) {
            throw new AccessDeniedException("You do not own this ticket");
        }

        if (ticket.getStatus() != TicketStatus.PAID) {
            throw new BusinessException("TICKET_NOT_VALIDATABLE",
                    "Ticket is not in a state that can generate a QR (status: " + ticket.getStatus() + ")");
        }

        // El evento debe seguir publicado: cancelado/finalizado → no se puede validar.
        EventRef event = eventLookupPort.find(ticket.getEventId())
                .orElseThrow(() -> new NotFoundException("EVENT_NOT_FOUND",
                        "Event not found for this ticket"));
        if (!event.isPublished()) {
            throw new BusinessException("EVENT_NOT_ACTIVE",
                    "The event is not active (status: " + event.status() + ")");
        }

        long ttl = qrTokenPort.getExpirationSeconds();
        String nonce = UUID.randomUUID().toString();

        // Reemplaza el nonce vigente → invalida cualquier QR anterior de este ticket.
        qrNonceStore.store(QrScope.TICKET, ticketId, nonce, ttl);

        String token = qrTokenPort.generate(
                QrScope.TICKET, ticketId, ticket.getEventId(), userId, nonce);

        return TicketQrResponse.builder()
                .scope(QrScope.TICKET.name())
                .targetId(ticketId)
                .qrToken(token)
                .expiresAt(LocalDateTime.now().plusSeconds(ttl))
                .expiresInSeconds(ttl)
                .build();
    }
}
