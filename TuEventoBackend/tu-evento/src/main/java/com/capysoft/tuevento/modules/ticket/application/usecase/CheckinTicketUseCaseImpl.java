package com.capysoft.tuevento.modules.ticket.application.usecase;

import com.capysoft.tuevento.modules.ticket.application.dto.response.TicketCheckinResponse;
import com.capysoft.tuevento.modules.ticket.domain.model.*;
import com.capysoft.tuevento.modules.ticket.domain.repository.TicketCheckinRepository;
import com.capysoft.tuevento.modules.ticket.domain.repository.TicketLogRepository;
import com.capysoft.tuevento.modules.ticket.domain.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Use case para hacer check-in de un ticket.
 * Valida que el ticket esté PAID y no tenga check-in previo.
 */
@Service
@RequiredArgsConstructor
public class CheckinTicketUseCaseImpl {
    
    private final TicketRepository ticketRepository;
    private final TicketCheckinRepository ticketCheckinRepository;
    private final TicketLogRepository ticketLogRepository;
    
    @Transactional
    public TicketCheckinResponse execute(Long ticketId, Long validatedBy) {
        Ticket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new TicketNotFoundException(ticketId));
        
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
