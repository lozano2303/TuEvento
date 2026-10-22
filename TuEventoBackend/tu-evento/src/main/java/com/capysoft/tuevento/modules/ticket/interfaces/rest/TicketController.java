package com.capysoft.tuevento.modules.ticket.interfaces.rest;

import com.capysoft.tuevento.modules.ticket.application.dto.request.CheckinTicketRequest;
import com.capysoft.tuevento.modules.ticket.application.dto.request.ScanQrRequest;
import com.capysoft.tuevento.modules.ticket.application.dto.response.ScanResultResponse;
import com.capysoft.tuevento.modules.ticket.application.dto.response.TicketCheckinResponse;
import com.capysoft.tuevento.modules.ticket.application.dto.response.TicketQrResponse;
import com.capysoft.tuevento.modules.ticket.application.dto.response.TicketResponse;
import com.capysoft.tuevento.modules.ticket.application.usecase.CheckinTicketUseCaseImpl;
import com.capysoft.tuevento.modules.ticket.application.usecase.GenerateTicketQrUseCaseImpl;
import com.capysoft.tuevento.modules.ticket.application.usecase.GetTicketUseCaseImpl;
import com.capysoft.tuevento.modules.ticket.application.usecase.GetUserTicketsUseCaseImpl;
import com.capysoft.tuevento.modules.ticket.application.usecase.ScanQrUseCaseImpl;
import com.capysoft.tuevento.shared.infrastructure.security.SecurityUser;
import com.capysoft.tuevento.shared.interfaces.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para tickets: consulta, QR, check-in y escaneo.
 */
@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketController {
    
    private final GetTicketUseCaseImpl getTicketUseCase;
    private final GetUserTicketsUseCaseImpl getUserTicketsUseCase;
    private final CheckinTicketUseCaseImpl checkinTicketUseCase;
    private final GenerateTicketQrUseCaseImpl generateTicketQrUseCase;
    private final ScanQrUseCaseImpl scanQrUseCase;
    
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
     * Obtener los tickets del usuario autenticado.
     */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<TicketResponse>>> getMyTickets(
            @AuthenticationPrincipal SecurityUser principal) {
        List<TicketResponse> tickets = getUserTicketsUseCase.execute(principal.getUserId().longValue());
        return ResponseEntity.ok(ApiResponse.ok("User tickets retrieved successfully", tickets));
    }

    /**
     * Generar el QR de un ticket individual (solo el comprador, solo en móvil).
     * El token caduca en segundos y regenerarlo invalida el anterior.
     */
    @PostMapping("/{ticketId}/qr")
    public ResponseEntity<ApiResponse<TicketQrResponse>> generateTicketQr(
            @PathVariable Long ticketId,
            @AuthenticationPrincipal SecurityUser principal) {
        TicketQrResponse response = generateTicketQrUseCase.execute(
                ticketId, principal.getUserId().longValue());
        return ResponseEntity.ok(ApiResponse.ok("Ticket QR generated", response));
    }

    /**
     * Escanear/validar un QR (solo el organizador dueño del evento).
     * Marca el ticket —o los tickets restantes de la orden— como USED.
     */
    @PostMapping("/scan")
    @PreAuthorize("hasAuthority('ORGANIZER')")
    public ResponseEntity<ApiResponse<ScanResultResponse>> scanQr(
            @Valid @RequestBody ScanQrRequest request,
            @AuthenticationPrincipal SecurityUser principal) {
        ScanResultResponse response = scanQrUseCase.execute(
                request.getQrToken(), principal.getUserId().longValue());
        return ResponseEntity.ok(ApiResponse.ok("QR scanned successfully", response));
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
