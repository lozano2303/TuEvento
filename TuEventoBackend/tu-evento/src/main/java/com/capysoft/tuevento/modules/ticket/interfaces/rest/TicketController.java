package com.capysoft.tuevento.modules.ticket.interfaces.rest;

import com.capysoft.tuevento.modules.ticket.application.dto.request.CheckinTicketRequest;
import com.capysoft.tuevento.modules.ticket.application.dto.response.TicketCheckinResponse;
import com.capysoft.tuevento.modules.ticket.application.dto.response.TicketResponse;
import com.capysoft.tuevento.modules.ticket.application.usecase.CheckinTicketUseCaseImpl;
import com.capysoft.tuevento.modules.ticket.application.usecase.GetTicketUseCaseImpl;
import com.capysoft.tuevento.modules.ticket.application.usecase.GetUserTicketsUseCaseImpl;
import com.capysoft.tuevento.shared.interfaces.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para gestión de tickets.
 */
@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketController {
    
    private final GetTicketUseCaseImpl getTicketUseCase;
    private final GetUserTicketsUseCaseImpl getUserTicketsUseCase;
    private final CheckinTicketUseCaseImpl checkinTicketUseCase;
    
    /**
     * Obtener un ticket por ID.
     */
    @GetMapping("/{ticketId}")
    public ResponseEntity<ApiResponse<TicketResponse>> getTicket(@PathVariable Long ticketId) {
        TicketResponse response = getTicketUseCase.execute(ticketId);
        return ResponseEntity.ok(ApiResponse.ok("Ticket retrieved successfully", response));
    }
    
    /**
     * Obtener los tickets de un usuario.
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<TicketResponse>>> getUserTickets(@PathVariable Long userId) {
        List<TicketResponse> tickets = getUserTicketsUseCase.execute(userId);
        return ResponseEntity.ok(ApiResponse.ok("User tickets retrieved successfully", tickets));
    }
    
    /**
     * Hacer check-in de un ticket (rol staff/organizer).
     */
    @PostMapping("/{ticketId}/checkin")
    public ResponseEntity<ApiResponse<TicketCheckinResponse>> checkinTicket(
            @PathVariable Long ticketId,
            @Valid @RequestBody CheckinTicketRequest request) {
        
        TicketCheckinResponse response = checkinTicketUseCase.execute(ticketId, request.getValidatedBy());
        return ResponseEntity.ok(ApiResponse.ok("Ticket checked in successfully", response));
    }
}
