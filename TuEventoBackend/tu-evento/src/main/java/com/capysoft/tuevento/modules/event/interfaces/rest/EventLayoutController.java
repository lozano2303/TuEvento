package com.capysoft.tuevento.modules.event.interfaces.rest;

import com.capysoft.tuevento.modules.event.application.dto.request.SaveEventLayoutRequest;
import com.capysoft.tuevento.modules.event.application.dto.response.EventLayoutResponse;
import com.capysoft.tuevento.modules.event.application.port.in.GetEventLayoutUseCase;
import com.capysoft.tuevento.modules.event.application.port.in.SaveEventLayoutUseCase;
import com.capysoft.tuevento.shared.infrastructure.security.SecurityUser;
import com.capysoft.tuevento.shared.interfaces.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/events/{eventId}/layout")
@RequiredArgsConstructor
@Tag(name = "Event Layout", description = "Event layout endpoints")
public class EventLayoutController {

    private final GetEventLayoutUseCase getEventLayoutUseCase;
    private final SaveEventLayoutUseCase saveEventLayoutUseCase;

    @Operation(summary = "Get layout for an event")
    @GetMapping
    public ResponseEntity<ApiResponse<EventLayoutResponse>> getLayout(
            @PathVariable Long eventId) {
        return ResponseEntity.ok(ApiResponse.ok("Layout retrieved successfully",
                getEventLayoutUseCase.findByEventId(eventId)));
    }

    @Operation(summary = "Create or update layout for an event")
    @PutMapping
    @PreAuthorize("hasAuthority('ORGANIZER')")
    public ResponseEntity<ApiResponse<EventLayoutResponse>> saveLayout(
            @PathVariable Long eventId,
            @Valid @RequestBody SaveEventLayoutRequest request,
            @AuthenticationPrincipal SecurityUser principal) {
        return ResponseEntity.ok(ApiResponse.ok("Layout saved successfully",
                saveEventLayoutUseCase.execute(eventId, request, principal.getUserId().longValue())));
    }
}
