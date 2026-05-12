package com.capysoft.tuevento.modules.event.interfaces.rest;

import com.capysoft.tuevento.modules.event.application.dto.request.ChangeEventStatusRequest;
import com.capysoft.tuevento.modules.event.application.dto.request.CreateEventRequest;
import com.capysoft.tuevento.modules.event.application.dto.request.UpdateEventRequest;
import com.capysoft.tuevento.modules.event.application.dto.response.EventResponse;
import com.capysoft.tuevento.modules.event.application.dto.response.EventStatusLogResponse;
import com.capysoft.tuevento.modules.event.application.dto.response.EventSummaryResponse;
import com.capysoft.tuevento.modules.event.application.port.in.ChangeEventStatusUseCase;
import com.capysoft.tuevento.modules.event.application.port.in.CreateEventUseCase;
import com.capysoft.tuevento.modules.event.application.port.in.DeleteEventUseCase;
import com.capysoft.tuevento.modules.event.application.port.in.GetEventUseCase;
import com.capysoft.tuevento.modules.event.application.port.in.UpdateEventUseCase;
import com.capysoft.tuevento.modules.event.domain.model.EventStatus;
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
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
@Tag(name = "Events", description = "Event management endpoints")
public class EventController {

    private final CreateEventUseCase      createEventUseCase;
    private final UpdateEventUseCase      updateEventUseCase;
    private final DeleteEventUseCase      deleteEventUseCase;
    private final GetEventUseCase         getEventUseCase;
    private final ChangeEventStatusUseCase changeEventStatusUseCase;

    @Operation(summary = "Create a new event")
    @PostMapping
    @PreAuthorize("hasAuthority('ORGANIZER')")
    public ResponseEntity<ApiResponse<EventResponse>> createEvent(
            @Valid @RequestBody CreateEventRequest request,
            @AuthenticationPrincipal SecurityUser principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Event created successfully",
                        createEventUseCase.execute(request, principal.getUserId().longValue())));
    }

    @Operation(summary = "Get event by ID")
    @GetMapping("/{eventId}")
    public ResponseEntity<ApiResponse<EventResponse>> getEvent(
            @PathVariable Long eventId) {
        return ResponseEntity.ok(ApiResponse.ok("Event retrieved successfully",
                getEventUseCase.findById(eventId)));
    }

    @Operation(summary = "Get events by user")
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<EventSummaryResponse>>> getEventsByUser(
            @PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.ok("Events retrieved successfully",
                getEventUseCase.findByUser(userId)));
    }

    @Operation(summary = "Get events by status")
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<EventSummaryResponse>>> getEventsByStatus(
            @PathVariable EventStatus status) {
        return ResponseEntity.ok(ApiResponse.ok("Events retrieved successfully",
                getEventUseCase.findByStatus(status)));
    }

    @Operation(summary = "Update an event")
    @PutMapping("/{eventId}")
    @PreAuthorize("hasAuthority('ORGANIZER')")
    public ResponseEntity<ApiResponse<EventResponse>> updateEvent(
            @PathVariable Long eventId,
            @Valid @RequestBody UpdateEventRequest request,
            @AuthenticationPrincipal SecurityUser principal) {
        return ResponseEntity.ok(ApiResponse.ok("Event updated successfully",
                updateEventUseCase.execute(eventId, request, principal.getUserId().longValue())));
    }

    @Operation(summary = "Delete an event")
    @DeleteMapping("/{eventId}")
    @PreAuthorize("hasAuthority('ORGANIZER')")
    public ResponseEntity<ApiResponse<Void>> deleteEvent(
            @PathVariable Long eventId,
            @AuthenticationPrincipal SecurityUser principal) {
        deleteEventUseCase.execute(eventId, principal.getUserId().longValue());
        return ResponseEntity.ok(ApiResponse.ok("Event deleted successfully"));
    }

    @Operation(summary = "Change event status")
    @PatchMapping("/{eventId}/status")
    @PreAuthorize("hasAuthority('ORGANIZER')")
    public ResponseEntity<ApiResponse<EventStatusLogResponse>> changeStatus(
            @PathVariable Long eventId,
            @Valid @RequestBody ChangeEventStatusRequest request,
            @AuthenticationPrincipal SecurityUser principal) {
        return ResponseEntity.ok(ApiResponse.ok("Event status changed successfully",
                changeEventStatusUseCase.execute(eventId, request, principal.getUserId().longValue())));
    }
}
