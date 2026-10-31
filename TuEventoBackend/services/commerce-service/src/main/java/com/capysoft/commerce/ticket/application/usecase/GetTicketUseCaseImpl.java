package com.capysoft.commerce.ticket.application.usecase;

import com.capysoft.commerce.ticket.application.dto.response.TicketResponse;
import com.capysoft.commerce.ticket.domain.model.Ticket;
import com.capysoft.commerce.ticket.domain.model.TicketNotFoundException;
import com.capysoft.commerce.ticket.domain.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case para obtener un ticket por ID.
 */
@Service
@RequiredArgsConstructor
public class GetTicketUseCaseImpl {
    
    private final TicketRepository ticketRepository;
    
    @Transactional(readOnly = true)
    public TicketResponse execute(Long ticketId, Long requesterId) {
        Ticket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new TicketNotFoundException(ticketId));
        if (!ticket.isOwnedBy(requesterId)) {
            throw new AccessDeniedException("You do not have access to this ticket");
        }
        
        return TicketResponse.fromDomain(ticket);
    }
}
