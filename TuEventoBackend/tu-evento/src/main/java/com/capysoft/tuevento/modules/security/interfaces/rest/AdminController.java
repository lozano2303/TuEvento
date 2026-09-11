package com.capysoft.tuevento.modules.security.interfaces.rest;

import com.capysoft.tuevento.modules.event.application.dto.request.ChangeEventStatusRequest;
import com.capysoft.tuevento.modules.event.application.dto.response.AdminEventSummaryResponse;
import com.capysoft.tuevento.modules.event.application.dto.response.EventStatusLogResponse;
import com.capysoft.tuevento.modules.event.application.port.in.AdminChangeEventStatusPort;
import com.capysoft.tuevento.modules.event.application.port.in.GetAdminEventsPort;
import com.capysoft.tuevento.modules.event.domain.model.EventStatus;
import com.capysoft.tuevento.modules.security.application.dto.response.OrganizerRequestResponse;
import com.capysoft.tuevento.modules.security.application.port.in.ApproveOrganizerRequestPort;
import com.capysoft.tuevento.modules.security.application.port.in.GetOrganizerRequestsPort;
import com.capysoft.tuevento.modules.security.application.port.in.RejectOrganizerRequestPort;
import com.capysoft.tuevento.modules.security.domain.model.OrganizerPetition;
import com.capysoft.tuevento.modules.security.domain.repository.OrganizerPetitionRepository;
import com.capysoft.tuevento.modules.storage.application.dto.response.PublicUrlResponse;
import com.capysoft.tuevento.modules.storage.application.port.in.GeneratePublicUrlPort;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
@Tag(name = "Admin", description = "Administration endpoints — requires ADMIN role")
public class AdminController {

    // ── Organizer requests ────────────────────────────────────────────────────
    private final GetOrganizerRequestsPort    getOrganizerRequestsPort;
    private final ApproveOrganizerRequestPort approveOrganizerRequestPort;
    private final RejectOrganizerRequestPort  rejectOrganizerRequestPort;
    private final GeneratePublicUrlPort       generatePublicUrlPort;
    private final OrganizerPetitionRepository organizerPetitionRepository;

    // ── Events ────────────────────────────────────────────────────────────────
    private final GetAdminEventsPort          getAdminEventsPort;
    private final AdminChangeEventStatusPort  adminChangeEventStatusPort;

    // ═════════════════════════════════════════════════════════════════════════
    // Organizer-request endpoints
    // ═════════════════════════════════════════════════════════════════════════

    @Operation(summary = "List all pending organizer requests")
    @GetMapping("/organizer-requests")
    public ResponseEntity<ApiResponse<List<OrganizerRequestResponse>>> getPendingRequests() {
        return ResponseEntity.ok(ApiResponse.ok("Pending organizer requests retrieved",
                getOrganizerRequestsPort.getPendingRequests()));
    }

    @Operation(summary = "Approve an organizer request and assign ORGANIZER role to the user")
    @PutMapping("/organizer-requests/{id}/approve")
    public ResponseEntity<ApiResponse<Void>> approve(@PathVariable Integer id) {
        approveOrganizerRequestPort.approve(id);
        return ResponseEntity.ok(ApiResponse.ok("Organizer request approved successfully"));
    }

    @Operation(summary = "Reject an organizer request")
    @PutMapping("/organizer-requests/{id}/reject")
    public ResponseEntity<ApiResponse<Void>> reject(@PathVariable Integer id) {
        rejectOrganizerRequestPort.reject(id);
        return ResponseEntity.ok(ApiResponse.ok("Organizer request rejected successfully"));
    }

    @Operation(summary = "Get document public URL for an organizer request")
    @GetMapping("/organizer-requests/{id}/document")
    public ResponseEntity<ApiResponse<PublicUrlResponse>> getDocumentUrl(@PathVariable Integer id) {
        OrganizerPetition petition = organizerPetitionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("PETITION_NOT_FOUND", "Organizer petition not found"));

        if (petition.getStoredFileId() == null) {
            throw new NotFoundException("FILE_NOT_FOUND", "Organizer petition does not have a document");
        }

        return ResponseEntity.ok(ApiResponse.ok("Document URL generated",
                generatePublicUrlPort.generate(petition.getStoredFileId())));
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Event-management endpoints
    // ═════════════════════════════════════════════════════════════════════════

    @Operation(summary = "List all events (all statuses). Pass ?status=DRAFT|PUBLISHED|CANCELLED|COMPLETED to filter.")
    @GetMapping("/events")
    public ResponseEntity<ApiResponse<List<AdminEventSummaryResponse>>> getEvents(
            @RequestParam(required = false) EventStatus status) {
        return ResponseEntity.ok(ApiResponse.ok("Events retrieved",
                getAdminEventsPort.getEvents(status)));
    }

    @Operation(summary = "Admin force-change event status — bypasses ownership check")
    @PatchMapping("/events/{eventId}/status")
    public ResponseEntity<ApiResponse<EventStatusLogResponse>> changeEventStatus(
            @PathVariable Long eventId,
            @Valid @RequestBody ChangeEventStatusRequest request,
            @AuthenticationPrincipal SecurityUser principal) {
        return ResponseEntity.ok(ApiResponse.ok("Event status changed",
                adminChangeEventStatusPort.execute(eventId, request, principal.getUserId().longValue())));
    }
}
