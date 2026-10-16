package com.capysoft.tuevento.modules.security.interfaces.rest;

import com.capysoft.tuevento.modules.event.application.dto.request.ChangeEventStatusRequest;
import com.capysoft.tuevento.modules.event.application.dto.response.AdminEventSummaryResponse;
import com.capysoft.tuevento.modules.event.application.dto.response.EventStatusLogResponse;
import com.capysoft.tuevento.modules.event.application.port.in.AdminChangeEventStatusPort;
import com.capysoft.tuevento.modules.event.application.port.in.GetAdminEventsPort;
import com.capysoft.tuevento.modules.event.domain.model.EventStatus;
import com.capysoft.tuevento.modules.event.infrastructure.persistence.repository.EventJpaRepository;
import com.capysoft.tuevento.modules.payment.infrastructure.persistence.repository.PaymentJpaRepository;
import com.capysoft.tuevento.modules.payment.infrastructure.persistence.repository.RefundJpaRepository;
import com.capysoft.tuevento.modules.payment.domain.model.PaymentStatus;
import com.capysoft.tuevento.modules.payment.domain.model.RefundStatus;
import com.capysoft.tuevento.modules.security.application.dto.response.AdminDashboardStatsResponse;
import com.capysoft.tuevento.modules.security.application.dto.response.OrganizerRequestResponse;
import com.capysoft.tuevento.modules.security.application.port.in.ApproveOrganizerRequestPort;
import com.capysoft.tuevento.modules.security.application.port.in.GetOrganizerRequestsPort;
import com.capysoft.tuevento.modules.security.application.port.in.RejectOrganizerRequestPort;
import com.capysoft.tuevento.modules.security.domain.model.OrganizerPetition;
import com.capysoft.tuevento.modules.security.domain.repository.OrganizerPetitionRepository;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.repository.UserJpaRepository;
import com.capysoft.tuevento.modules.storage.application.dto.response.PublicUrlResponse;
import com.capysoft.tuevento.modules.storage.application.port.in.GeneratePublicUrlPort;
import com.capysoft.tuevento.modules.ticket.infrastructure.persistence.repository.TicketJpaRepository;
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

    // ── Stats (JPA repos injected directly — read-only aggregates) ────────────
    private final UserJpaRepository           userJpaRepository;
    private final EventJpaRepository          eventJpaRepository;
    private final TicketJpaRepository         ticketJpaRepository;
    private final PaymentJpaRepository        paymentJpaRepository;
    private final RefundJpaRepository         refundJpaRepository;

    // ═════════════════════════════════════════════════════════════════════════
    // Dashboard stats
    // ═════════════════════════════════════════════════════════════════════════

    @Operation(summary = "Get aggregated platform stats for the admin dashboard")
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminDashboardStatsResponse>> getStats() {
        var stats = AdminDashboardStatsResponse.builder()
                // users
                .totalUsers(userJpaRepository.count())
                // events
                .totalEvents(eventJpaRepository.count())
                .publishedEvents(eventJpaRepository.countByStatus(EventStatus.PUBLISHED))
                .pendingReviewEvents(eventJpaRepository.countByStatus(EventStatus.PENDING_REVIEW))
                .rejectedEvents(eventJpaRepository.countByStatus(EventStatus.REJECTED))
                .cancelledEvents(eventJpaRepository.countByStatus(EventStatus.CANCELLED))
                .completedEvents(eventJpaRepository.countByStatus(EventStatus.COMPLETED))
                .draftEvents(eventJpaRepository.countByStatus(EventStatus.DRAFT))
                // tickets
                .totalTickets(ticketJpaRepository.count())
                // organizer requests
                .pendingOrganizerRequests(organizerPetitionRepository.findByStatus("PENDING").size())
                .approvedOrganizerRequests(organizerPetitionRepository.findByStatus("APPROVED").size())
                // payments
                .totalRevenue(paymentJpaRepository.sumApprovedRevenue())
                .approvedPayments(paymentJpaRepository.countByStatus(PaymentStatus.APPROVED))
                .pendingRefunds(refundJpaRepository.countByStatus(RefundStatus.REQUESTED))
                .build();
        return ResponseEntity.ok(ApiResponse.ok("Dashboard stats retrieved", stats));
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Organizer-request endpoints
    // ═════════════════════════════════════════════════════════════════════════

    @Operation(summary = "List organizer requests. Pass ?status=PENDING|APPROVED|REJECTED to filter; omit for all.")
    @GetMapping("/organizer-requests")
    public ResponseEntity<ApiResponse<List<OrganizerRequestResponse>>> getOrganizerRequests(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponse.ok("Organizer requests retrieved",
                getOrganizerRequestsPort.getRequests(status)));
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

    @Operation(summary = "List all events (all statuses). Pass ?status=DRAFT|PENDING_REVIEW|PUBLISHED|REJECTED|CANCELLED|COMPLETED to filter.")
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
