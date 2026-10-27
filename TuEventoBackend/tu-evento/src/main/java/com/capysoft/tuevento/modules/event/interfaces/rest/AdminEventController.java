package com.capysoft.tuevento.modules.event.interfaces.rest;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.capysoft.tuevento.modules.event.application.dto.request.ChangeEventStatusRequest;
import com.capysoft.tuevento.modules.event.application.dto.response.AdminEventSummaryResponse;
import com.capysoft.tuevento.modules.event.application.dto.response.EventStatusLogResponse;
import com.capysoft.tuevento.modules.event.application.port.in.AdminChangeEventStatusPort;
import com.capysoft.tuevento.modules.event.application.port.in.GetAdminEventsPort;
import com.capysoft.tuevento.modules.event.domain.model.EventStatus;
import com.capysoft.tuevento.shared.infrastructure.security.SecurityUser;
import com.capysoft.tuevento.shared.interfaces.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Administración de eventos. Vive en Catálogo; antes estaba en el AdminController de Identidad. */
@RestController
@RequestMapping("/api/v1/admin/events")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
@Tag(name = "Admin", description = "Administration endpoints — requires ADMIN role")
public class AdminEventController {

    private final GetAdminEventsPort         getAdminEventsPort;
    private final AdminChangeEventStatusPort adminChangeEventStatusPort;

    @Operation(summary = "List all events (all statuses). Pass ?status=DRAFT|PENDING_REVIEW|PUBLISHED|REJECTED|CANCELLED|COMPLETED to filter.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminEventSummaryResponse>>> getEvents(
            @RequestParam(required = false) EventStatus status) {
        return ResponseEntity.ok(ApiResponse.ok("Events retrieved",
                getAdminEventsPort.getEvents(status)));
    }

    @Operation(summary = "Admin force-change event status — bypasses ownership check")
    @PatchMapping("/{eventId}/status")
    public ResponseEntity<ApiResponse<EventStatusLogResponse>> changeEventStatus(
            @PathVariable Long eventId,
            @Valid @RequestBody ChangeEventStatusRequest request,
            @AuthenticationPrincipal SecurityUser principal) {
        return ResponseEntity.ok(ApiResponse.ok("Event status changed",
                adminChangeEventStatusPort.execute(eventId, request, principal.getUserId().longValue())));
    }
}
