package com.capysoft.tuevento.modules.section.interfaces.rest;

import com.capysoft.tuevento.modules.section.application.dto.request.CreateEventSectionRequest;
import com.capysoft.tuevento.modules.section.application.dto.request.UpdateEventSectionRequest;
import com.capysoft.tuevento.modules.section.application.dto.response.EventSectionResponse;
import com.capysoft.tuevento.modules.section.application.port.in.EventSectionUseCase;
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
@RequestMapping("/api/v1/event-sections")
@RequiredArgsConstructor
@Tag(name = "Event Sections", description = "Event section management endpoints")
public class EventSectionController {

    private final EventSectionUseCase eventSectionUseCase;

    @Operation(summary = "Get sections for an event — public")
    @GetMapping("/event/{eventId}")
    public ResponseEntity<ApiResponse<List<EventSectionResponse>>> getByEvent(
            @PathVariable Integer eventId,
            @RequestParam(required = false) String languageCode) {
        return ResponseEntity.ok(ApiResponse.ok("Secciones del evento obtenidas correctamente",
                eventSectionUseCase.getSectionsByEvent(eventId, languageCode)));
    }

    @Operation(summary = "Create a section for an event")
    // Solo el organizador dueño del evento (ADMIN solo consulta, igual que el layout)
    @PostMapping
    @PreAuthorize("hasAuthority('ORGANIZER')")
    public ResponseEntity<ApiResponse<EventSectionResponse>> create(
            @Valid @RequestBody CreateEventSectionRequest request,
            @AuthenticationPrincipal SecurityUser principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Sección creada correctamente",
                        eventSectionUseCase.createEventSection(request, principal.getUserId().longValue())));
    }

    @Operation(summary = "Update a section")
    @PutMapping("/{eventSectionId}")
    @PreAuthorize("hasAuthority('ORGANIZER')")
    public ResponseEntity<ApiResponse<EventSectionResponse>> update(
            @PathVariable Integer eventSectionId,
            @Valid @RequestBody UpdateEventSectionRequest request,
            @AuthenticationPrincipal SecurityUser principal) {
        return ResponseEntity.ok(ApiResponse.ok("Sección actualizada correctamente",
                eventSectionUseCase.updateEventSection(eventSectionId, request, principal.getUserId().longValue())));
    }

    @Operation(summary = "Delete a section")
    @DeleteMapping("/{eventSectionId}")
    @PreAuthorize("hasAuthority('ORGANIZER')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Integer eventSectionId,
            @AuthenticationPrincipal SecurityUser principal) {
        eventSectionUseCase.deleteEventSection(eventSectionId, principal.getUserId().longValue());
        return ResponseEntity.ok(ApiResponse.ok("Sección eliminada correctamente"));
    }
}
