package com.capysoft.tuevento.modules.seat.interfaces.rest;

import com.capysoft.tuevento.modules.seat.application.dto.request.CreateSeatRequest;
import com.capysoft.tuevento.modules.seat.application.dto.request.UpdateSeatStatusRequest;
import com.capysoft.tuevento.modules.seat.application.dto.response.SeatLogResponse;
import com.capysoft.tuevento.modules.seat.application.dto.response.SeatResponse;
import com.capysoft.tuevento.modules.seat.application.port.in.SeatUseCase;
import com.capysoft.tuevento.shared.infrastructure.security.SecurityUser;
import com.capysoft.tuevento.shared.interfaces.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/seats")
@RequiredArgsConstructor
@Tag(name = "Seats", description = "Seat management endpoints")
public class SeatController {

    private final SeatUseCase seatUseCase;

    @Operation(summary = "Get all seats by block — public")
    @GetMapping("/block/{seatBlockId}")
    public ResponseEntity<ApiResponse<List<SeatResponse>>> getByBlock(
            @PathVariable Integer seatBlockId) {
        return ResponseEntity.ok(ApiResponse.ok("Sillas obtenidas correctamente",
                seatUseCase.getSeatsByBlock(seatBlockId)));
    }

    @Operation(summary = "Get all seats by section — public")
    @GetMapping("/section/{eventSectionId}")
    public ResponseEntity<ApiResponse<List<SeatResponse>>> getBySection(
            @PathVariable Integer eventSectionId) {
        return ResponseEntity.ok(ApiResponse.ok("Sillas obtenidas correctamente",
                seatUseCase.getSeatsBySection(eventSectionId)));
    }

    @Operation(summary = "Get seat status change log — public")
    @GetMapping("/{seatId}/log")
    public ResponseEntity<ApiResponse<List<SeatLogResponse>>> getLog(
            @PathVariable Integer seatId) {
        return ResponseEntity.ok(ApiResponse.ok("Historial obtenido correctamente",
                seatUseCase.getSeatLog(seatId)));
    }

    // Escrituras: solo el organizador del evento (y en DRAFT); ADMIN solo consulta, igual que en las secciones
    @Operation(summary = "Create a new seat")
    @PostMapping
    @PreAuthorize("hasAuthority('ORGANIZER')")
    public ResponseEntity<ApiResponse<SeatResponse>> create(
            @Valid @RequestBody CreateSeatRequest request,
            @AuthenticationPrincipal SecurityUser principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Silla creada correctamente",
                        seatUseCase.createSeat(request, principal.getUserId().longValue())));
    }

    @Operation(summary = "Mark or unmark a seat as courtesy")
    @PatchMapping("/{seatId}/status")
    @PreAuthorize("hasAuthority('ORGANIZER')")
    public ResponseEntity<ApiResponse<SeatResponse>> updateStatus(
            @PathVariable Integer seatId,
            @Valid @RequestBody UpdateSeatStatusRequest request,
            @AuthenticationPrincipal SecurityUser principal) {
        return ResponseEntity.ok(ApiResponse.ok("Estado actualizado correctamente",
                seatUseCase.updateSeatStatusByOrganizer(seatId, request, principal.getUserId().longValue())));
    }

    @Operation(summary = "Delete a seat")
    @DeleteMapping("/{seatId}")
    @PreAuthorize("hasAuthority('ORGANIZER')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Integer seatId,
            @AuthenticationPrincipal SecurityUser principal) {
        seatUseCase.deleteSeat(seatId, principal.getUserId().longValue());
        return ResponseEntity.ok(ApiResponse.ok("Silla eliminada correctamente"));
    }
}
