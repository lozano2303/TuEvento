package com.capysoft.commerce.ticket.application.usecase;

import com.capysoft.commerce.ticket.application.dto.response.TicketResponse;
import com.capysoft.commerce.ticket.domain.model.Ticket;
import com.capysoft.commerce.ticket.domain.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Use case para obtener los tickets de un usuario.
 */
@Service
@RequiredArgsConstructor
public class GetUserTicketsUseCaseImpl {
    
    private final TicketRepository ticketRepository;
    
    @Transactional(readOnly = true)
    public List<TicketResponse> execute(Long userId) {
        List<Ticket> tickets = ticketRepository.findByUserId(userId);
        
        return tickets.stream()
            .map(TicketResponse::fromDomain)
            .collect(Collectors.toList());
    }

    /**
     * Tickets de otro usuario: solo el propio usuario o un administrador.
     */
    @Transactional(readOnly = true)
    public List<TicketResponse> executeFor(Long userId, Long requesterId, boolean requesterIsAdmin) {
        if (!requesterIsAdmin && !userId.equals(requesterId)) {
            throw new AccessDeniedException("You do not have access to these tickets");
        }
        return execute(userId);
    }
}
