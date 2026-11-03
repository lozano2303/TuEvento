package com.capysoft.commerce.ticket.application.usecase;

import com.capysoft.commerce.ticket.application.port.out.EventLookupPort;
import com.capysoft.commerce.ticket.application.dto.response.TicketCheckinResponse;
import com.capysoft.commerce.ticket.domain.model.*;
import com.capysoft.commerce.ticket.domain.repository.TicketCheckinRepository;
import com.capysoft.commerce.ticket.domain.repository.TicketLogRepository;
import com.capysoft.commerce.ticket.domain.repository.TicketRepository;
import com.capysoft.common.domain.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Use case para hacer check-in de un ticket.
 * Valida que el ticket esté PAID y no tenga check-in previo.
 * Solo el organizador dueño del evento puede hacerlo; el validador es el
 * usuario autenticado, nunca un valor enviado por el cliente.
 */
@Service
@RequiredArgsConstructor
public class CheckinTicketUseCaseImpl {
    
    private final TicketRepository ticketRepository;
    private final TicketCheckinRepository ticketCheckinRepository;
    private final TicketLogRepository ticketLogRepository;
    private final EventLookupPort eventLookupPort;
    
    @Transactional
    public TicketCheckinResponse execute(Long ticketId, Long validatedBy) {
        // Lectura con bloqueo: no se puede usar un ticket mientras se reembolsa (y viceversa)
        Ticket ticket = ticketRepository.findByIdForUpdate(ticketId)
            .orElseThrow(() -> new TicketNotFoundException(ticketId));

        // Autorizar: el organizador autenticado debe ser el dueño del evento del ticket
        Long eventOwnerId = eventLookupPort.find(ticket.getEventId())
            .orElseThrow(() -> new NotFoundException("EVENT_NOT_FOUND", "Event not found for this ticket"))
            .ownerId();
        if (!eventOwnerId.equals(validatedBy)) {
            throw new AccessDeniedException("You are not the organizer of this event");
        }
        
        // Validar que el ticket pueda hacer check-in
        if (!ticket.canCheckin()) {
            throw new IllegalStateException(
                "Ticket cannot check-in. Status: " + ticket.getStatus() + 
                ", Expiration: " + ticket.getExpirationDate()
            );
        }
        
        // Validar que no tenga check-in previo
        if (ticketCheckinRepository.existsByTicketId(ticketId)) {
            throw new IllegalStateException("Ticket already checked in");
        }
        
        // Crear check-in
        TicketCheckin checkin = TicketCheckin.builder()
            .ticketId(ticketId)
            .checkinTime(LocalDateTime.now())
            .validatedBy(validatedBy)
            .build();
        
        TicketCheckin savedCheckin = ticketCheckinRepository.save(checkin);
        
        // Marcar ticket como USED
        TicketStatus oldStatus = ticket.getStatus();
        ticket.markAsUsed();
        ticketRepository.save(ticket);
        
        // Log de auditoría
        TicketLog log = TicketLog.builder()
            .ticketId(ticketId)
            .oldStatus(oldStatus)
            .newStatus(TicketStatus.USED)
            .changedAt(LocalDateTime.now())
            .changedBy(validatedBy.toString())
            .reason("Check-in completed")
            .build();
        ticketLogRepository.save(log);
        
        return TicketCheckinResponse.fromDomain(savedCheckin);
    }
}
