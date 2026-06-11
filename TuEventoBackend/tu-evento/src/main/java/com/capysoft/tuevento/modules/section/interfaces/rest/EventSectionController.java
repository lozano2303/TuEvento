package com.capysoft.tuevento.modules.section.interfaces.rest;

import com.capysoft.tuevento.modules.section.application.dto.request.CreateEventSectionRequest;
import com.capysoft.tuevento.modules.section.application.dto.request.UpdateEventSectionRequest;
import com.capysoft.tuevento.modules.section.application.dto.response.EventSectionResponse;
import com.capysoft.tuevento.modules.section.application.port.in.EventSectionUseCase;
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
@RequestMapping("/api/v1/event-sections")
@RequiredArgsConstructor
@Tag(name = "Event Sections", description = "Event section management endpoints")
public class EventSectionController {

    private final EventSectionUseCase eventSectionUseCase;

    @Operation(summary = "Get sections for an event — public")
    @GetMapping("/event/{eventId}")
    public ResponseEntity<ApiResponse<List<EventSectionResponse>>> getByEvent(
            @PathVariable Integer eventId) {
        return ResponseEntity.ok(ApiResponse.ok("Secciones del evento obtenidas correctamente",
                eventSectionUseCase.getSectionsByEvent(eventId)));
    }

    @Operation(summary = "Create a section for an event")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ORGANIZER', 'ADMIN')")
    public ResponseEntity<ApiResponse<EventSectionResponse>> create(
            @Valid @RequestBody CreateEventSectionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Sección creada correctamente",
                        eventSectionUseCase.createEventSection(request)));
    }

    @Operation(summary = "Update a section")
    @PutMapping("/{eventSectionId}")
    @PreAuthorize("hasAnyAuthority('ORGANIZER', 'ADMIN')")
    public ResponseEntity<ApiResponse<EventSectionResponse>> update(
            @PathVariable Integer eventSectionId,
            @Valid @RequestBody UpdateEventSectionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Sección actualizada correctamente",
                eventSectionUseCase.updateEventSection(eventSectionId, request)));
    }

    @Operation(summary = "Delete a section")
    @DeleteMapping("/{eventSectionId}")
    @PreAuthorize("hasAnyAuthority('ORGANIZER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Integer eventSectionId) {
        eventSectionUseCase.deleteEventSection(eventSectionId);
        return ResponseEntity.ok(ApiResponse.ok("Sección eliminada correctamente"));
    }
}
