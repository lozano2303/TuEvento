package com.capysoft.tuevento.modules.event.interfaces.rest;

import com.capysoft.tuevento.modules.event.application.dto.response.EventLayoutResponse;
import com.capysoft.tuevento.modules.event.application.port.in.GetEventLayoutUseCase;
import com.capysoft.tuevento.shared.interfaces.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/events/{eventId}/layout")
@RequiredArgsConstructor
@Tag(name = "Event Layout", description = "Event layout endpoints")
public class EventLayoutController {

    private final GetEventLayoutUseCase getEventLayoutUseCase;

    @Operation(summary = "Get layout for an event")
    @GetMapping
    public ResponseEntity<ApiResponse<EventLayoutResponse>> getLayout(
            @PathVariable Long eventId) {
        return ResponseEntity.ok(ApiResponse.ok("Layout retrieved successfully",
                getEventLayoutUseCase.findByEventId(eventId)));
    }
}
