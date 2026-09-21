package com.capysoft.tuevento.modules.seat.interfaces.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.capysoft.tuevento.modules.seat.application.dto.response.SeatResponse;
import com.capysoft.tuevento.modules.seat.application.usecase.SeatReservationService;
import com.capysoft.tuevento.shared.infrastructure.security.SecurityUser;
import com.capysoft.tuevento.shared.interfaces.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Controller para gestionar reservas temporales de sillas.
 * 
 * Endpoints públicos (requieren autenticación, cualquier rol puede usarlos):
 * - POST /api/v1/seats/{seatId}/reserve - Reservar una silla por 10 minutos
 * - POST /api/v1/seats/{seatId}/release - Liberar una reserva propia antes de que expire
 * 
 * Las reservas tienen un TTL de 10 minutos. Pasado ese tiempo, se liberan
 * automáticamente via SeatReservationExpirationScheduler.
 */
@RestController
@RequestMapping("/api/v1/seats")
@RequiredArgsConstructor
@Tag(name = "Seat Reservations", description = "Temporary seat reservation endpoints")
public class SeatReservationController {

    private final SeatReservationService reservationService;

    /**
     * Reserva una silla temporalmente para el usuario autenticado.
     * 
     * La silla debe estar en estado AVAILABLE. La reserva expira después de 10 minutos,
     * tras lo cual vuelve automáticamente a AVAILABLE.
     * 
     * Usuarios conectados vía WebSocket en /topic/events/{eventId}/seats recibirán
     * una notificación en tiempo real del cambio de estado.
     * 
     * @param seatId ID de la silla a reservar
     * @param principal Usuario autenticado
     * @return Silla reservada con reserved_by y reserved_until poblados
     */
    @Operation(summary = "Reserve a seat temporarily (10 min TTL)")
    @PostMapping("/{seatId}/reserve")
    @PreAuthorize("isAuthenticated()") // cualquier usuario autenticado puede reservar
    public ResponseEntity<ApiResponse<SeatResponse>> reserve(
            @PathVariable Integer seatId,
            @AuthenticationPrincipal SecurityUser principal) {
        
        SeatResponse reserved = reservationService.reserveSeat(seatId, principal.getUserId());
        
        return ResponseEntity.ok(ApiResponse.ok(
                "Silla reservada exitosamente por 10 minutos", reserved));
    }

    /**
     * Libera una reserva de silla antes de su expiración automática.
     * 
     * Solo el usuario que realizó la reserva puede liberarla, excepto ADMIN que
     * puede liberar cualquier reserva.
     * 
     * Usuarios conectados vía WebSocket recibirán una notificación en tiempo real.
     * 
     * @param seatId ID de la silla a liberar
     * @param principal Usuario autenticado
     * @return Silla liberada (AVAILABLE, reserved_by y reserved_until en null)
     */
    @Operation(summary = "Release a seat reservation manually")
    @PostMapping("/{seatId}/release")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SeatResponse>> release(
            @PathVariable Integer seatId,
            @AuthenticationPrincipal SecurityUser principal) {
        
        boolean isAdmin = "ADMIN".equals(principal.getRole());
        
        SeatResponse released = reservationService.releaseSeat(
                seatId, principal.getUserId(), isAdmin);
        
        return ResponseEntity.ok(ApiResponse.ok(
                "Reserva liberada exitosamente", released));
    }
}
