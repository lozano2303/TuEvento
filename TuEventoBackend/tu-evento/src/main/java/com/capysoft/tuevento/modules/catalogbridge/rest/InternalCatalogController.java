package com.capysoft.tuevento.modules.catalogbridge.rest;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.capysoft.tuevento.modules.catalogbridge.application.HoldSeatsForOrderService;
import com.capysoft.tuevento.modules.catalogbridge.application.HoldSeatsForOrderService.HeldSeats;
import com.capysoft.tuevento.modules.catalogbridge.application.HoldSeatsForOrderService.HoldRejectedException;
import com.capysoft.tuevento.modules.seat.domain.model.EventSeatSummary;
import com.capysoft.tuevento.modules.seat.domain.repository.SeatRepository;
import com.capysoft.tuevento.shared.interfaces.ApiResponse;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

/**
 * API interna de Catálogo (sección 4.1 del plan), para commerce-service. Cuando exista catalog-service
 * la expondrá él con el mismo contrato. No la usan web ni móvil y el gateway no la enruta; exige la
 * clave entre servicios ({@code InternalApiKeyFilter}).
 */
@Hidden
@RestController
@RequiredArgsConstructor
public class InternalCatalogController {

    private final HoldSeatsForOrderService holdSeats;
    private final SeatRepository seatRepository;

    public record HoldRequest(@NotNull Long orderId, @NotNull Long userId, @NotNull Long eventId,
                              @NotEmpty List<Integer> seatIds) {
    }

    @PostMapping("/internal/seats/hold")
    public ResponseEntity<ApiResponse<HeldSeats>> hold(@Valid @RequestBody HoldRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Seats held",
                holdSeats.hold(request.userId(), request.eventId(), request.seatIds())));
    }

    @GetMapping("/internal/organizers/{organizerId}/seat-summary")
    public ResponseEntity<ApiResponse<List<EventSeatSummary>>> seatSummary(@PathVariable Long organizerId) {
        return ResponseEntity.ok(ApiResponse.ok("Seat summary retrieved",
                seatRepository.summarizeSeatsByOrganizer(organizerId)));
    }

    @ExceptionHandler(HoldRejectedException.class)
    public ResponseEntity<ApiResponse<Void>> onHoldRejected(HoldRejectedException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(e.getCode(), e.getMessage()));
    }
}
