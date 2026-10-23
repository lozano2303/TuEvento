package com.capysoft.tuevento.modules.seat.interfaces.rest;

import com.capysoft.tuevento.modules.seat.application.dto.request.CreateSeatBlockRequest;
import com.capysoft.tuevento.modules.seat.application.dto.response.SeatBlockResponse;
import com.capysoft.tuevento.modules.seat.application.port.in.SeatBlockUseCase;
import com.capysoft.tuevento.shared.interfaces.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/seat-blocks")
@RequiredArgsConstructor
@Tag(name = "Seat Blocks", description = "Seat block management endpoints")
public class SeatBlockController {

    private final SeatBlockUseCase seatBlockUseCase;

    @Operation(summary = "Get all seat blocks by section — public")
    @GetMapping("/section/{eventSectionId}")
    public ResponseEntity<ApiResponse<List<SeatBlockResponse>>> getBySection(
            @PathVariable Integer eventSectionId) {
        return ResponseEntity.ok(ApiResponse.ok("Bloques obtenidos correctamente",
                seatBlockUseCase.getBlocksBySection(eventSectionId)));
    }

    @Operation(summary = "Create a new seat block")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ORGANIZER', 'ADMIN')")
    public ResponseEntity<ApiResponse<SeatBlockResponse>> create(
            @Valid @RequestBody CreateSeatBlockRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Bloque creado correctamente",
                        seatBlockUseCase.createSeatBlock(request)));
    }

    @Operation(summary = "Delete a seat block")
    @DeleteMapping("/{seatBlockId}")
    @PreAuthorize("hasAnyAuthority('ORGANIZER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Integer seatBlockId) {
        seatBlockUseCase.deleteSeatBlock(seatBlockId);
        return ResponseEntity.ok(ApiResponse.ok("Bloque eliminado correctamente"));
    }
}
