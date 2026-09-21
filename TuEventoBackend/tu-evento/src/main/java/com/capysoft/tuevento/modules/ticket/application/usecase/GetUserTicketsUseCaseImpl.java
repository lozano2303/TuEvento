package com.capysoft.tuevento.modules.ticket.application.usecase;

import com.capysoft.tuevento.modules.ticket.application.dto.response.TicketResponse;
import com.capysoft.tuevento.modules.ticket.domain.model.Ticket;
import com.capysoft.tuevento.modules.ticket.domain.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
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
}
