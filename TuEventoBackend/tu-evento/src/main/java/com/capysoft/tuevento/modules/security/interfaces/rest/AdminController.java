package com.capysoft.tuevento.modules.security.interfaces.rest;

import com.capysoft.tuevento.modules.event.application.dto.request.ChangeEventStatusRequest;
import com.capysoft.tuevento.modules.event.application.dto.response.AdminEventSummaryResponse;
import com.capysoft.tuevento.modules.event.application.dto.response.EventStatusLogResponse;
import com.capysoft.tuevento.modules.event.application.port.in.AdminChangeEventStatusPort;
import com.capysoft.tuevento.modules.event.application.port.in.GetAdminEventsPort;
import com.capysoft.tuevento.modules.event.domain.model.EventStatus;
import com.capysoft.tuevento.modules.event.infrastructure.persistence.repository.EventJpaRepository;
import com.capysoft.tuevento.modules.payment.infrastructure.persistence.repository.PaymentJpaRepository;
import com.capysoft.tuevento.modules.wallet.application.dto.BulkCreditWalletRequest;
import com.capysoft.tuevento.modules.wallet.application.dto.BulkCreditWalletResponse;
import com.capysoft.tuevento.modules.wallet.application.usecase.BulkCreditWalletUseCaseImpl;
import com.capysoft.tuevento.modules.payment.infrastructure.persistence.repository.RefundJpaRepository;
import com.capysoft.tuevento.modules.payment.domain.model.PaymentStatus;
import com.capysoft.tuevento.modules.payment.domain.model.RefundStatus;
import com.capysoft.tuevento.modules.profile.infrastructure.persistence.repository.ProfileJpaRepository;
import com.capysoft.tuevento.modules.security.application.dto.response.ActivityItemResponse;
import com.capysoft.tuevento.modules.security.application.dto.response.AdminDashboardStatsResponse;
import com.capysoft.tuevento.modules.security.application.dto.response.AdminPendingCountsResponse;
import com.capysoft.tuevento.modules.security.application.dto.response.OrganizerRequestResponse;
import com.capysoft.tuevento.modules.security.application.dto.response.OrganizerUserResponse;
import com.capysoft.tuevento.modules.security.application.port.in.ApproveOrganizerRequestPort;
import com.capysoft.tuevento.modules.security.application.port.in.GetAdminPendingCountsPort;
import com.capysoft.tuevento.modules.security.application.port.in.GetOrganizerRequestsPort;
import com.capysoft.tuevento.modules.security.application.port.in.GetOrganizersPort;
import com.capysoft.tuevento.modules.security.application.port.in.RejectOrganizerRequestPort;
import com.capysoft.tuevento.modules.security.application.port.in.RevokeOrganizerRolePort;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private final GetAdminPendingCountsPort   getAdminPendingCountsPort;
    private final GeneratePublicUrlPort       generatePublicUrlPort;
    private final OrganizerPetitionRepository organizerPetitionRepository;

    // ── Organizer role management ─────────────────────────────────────────────
    private final GetOrganizersPort           getOrganizersPort;
    private final RevokeOrganizerRolePort     revokeOrganizerRolePort;

    // ── Events ────────────────────────────────────────────────────────────────
    private final GetAdminEventsPort          getAdminEventsPort;
    private final AdminChangeEventStatusPort  adminChangeEventStatusPort;

    // ── Stats (JPA repos injected directly — read-only aggregates) ────────────
    private final UserJpaRepository           userJpaRepository;
    private final EventJpaRepository          eventJpaRepository;
    private final TicketJpaRepository         ticketJpaRepository;
    private final PaymentJpaRepository        paymentJpaRepository;
    private final RefundJpaRepository         refundJpaRepository;
    private final ProfileJpaRepository        profileJpaRepository;

    // ── Wallet bulk credit ────────────────────────────────────────────────────
    private final BulkCreditWalletUseCaseImpl bulkCreditWalletUseCase;

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
                .totalRefundedAmount(paymentJpaRepository.sumTotalRefundedAmount())
                .processedRefunds(refundJpaRepository.countByStatus(RefundStatus.PROCESSED))
                .build();
        return ResponseEntity.ok(ApiResponse.ok("Dashboard stats retrieved", stats));
    }

    @Operation(summary = "Get counts of items awaiting admin action (organizer requests and events in review). "
            + "Live updates are pushed to /topic/admin/pending-counts")
    @GetMapping("/pending-counts")
    public ResponseEntity<ApiResponse<AdminPendingCountsResponse>> getPendingCounts() {
        return ResponseEntity.ok(ApiResponse.ok("Pending counts retrieved", getAdminPendingCountsPort.getCounts()));
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
    // Organizer role management endpoints
    // ═════════════════════════════════════════════════════════════════════════

    @Operation(summary = "List all users currently holding the ORGANIZER role")
    @GetMapping("/organizer-users")
    public ResponseEntity<ApiResponse<List<OrganizerUserResponse>>> getOrganizers() {
        return ResponseEntity.ok(ApiResponse.ok("Organizers retrieved",
                getOrganizersPort.getOrganizers()));
    }

    @Operation(summary = "Revoke the ORGANIZER role from a user (downgrades to USER). " +
               "Sends in-app + email notification and pushes a WebSocket role-change signal.")
    @DeleteMapping("/organizer-users/{userId}/revoke")
    public ResponseEntity<ApiResponse<Void>> revokeOrganizerRole(@PathVariable Integer userId) {
        revokeOrganizerRolePort.revoke(userId);
        return ResponseEntity.ok(ApiResponse.ok("Organizer role revoked successfully"));
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

    // ═════════════════════════════════════════════════════════════════════════
    // Activity feed
    // ═════════════════════════════════════════════════════════════════════════

    @Operation(summary = "Get unified admin activity feed — combines recent events, payments and user registrations")
    @GetMapping("/activity")
    public ResponseEntity<ApiResponse<List<ActivityItemResponse>>> getActivity(
            @RequestParam(defaultValue = "50") int limit) {

        List<ActivityItemResponse> items = new ArrayList<>();

        // ── Recent event status changes ───────────────────────────────────────
        var recentEvents = eventJpaRepository.findTopByOrderByUpdatedAtDesc(
                PageRequest.of(0, Math.min(limit, 100)));

        // Collect organizer userIds to resolve names in one query
        var organizerIds = recentEvents.stream()
                .map(e -> e.getUserId().intValue())
                .distinct()
                .toList();

        Map<Integer, String> organizerNames = profileJpaRepository
                .findAllByUserIdIn(organizerIds)
                .stream()
                .collect(Collectors.toMap(
                        p -> p.getUserId(),
                        p -> p.getFullName(),
                        (a, b) -> a));

        for (var ev : recentEvents) {
            String organizerName = organizerNames.getOrDefault(ev.getUserId().intValue(), "Organizador");
            String title = switch (ev.getStatus()) {
                case PUBLISHED       -> "Event approved and published";
                case REJECTED        -> "Event rejected";
                case CANCELLED       -> "Event cancelled";
                case PENDING_REVIEW  -> "Event submitted for review";
                case COMPLETED       -> "Event completed";
                default              -> "Event created as draft";
            };
            items.add(ActivityItemResponse.builder()
                    .type("EVENT_STATUS_CHANGE")
                    .title(title)
                    .description("\"" + ev.getEventName() + "\"")
                    .actor("Organizer: " + organizerName)
                    .occurredAt(ev.getUpdatedAt() != null ? ev.getUpdatedAt() : ev.getCreatedAt())
                    .build());
        }

        // ── Recent approved payments ──────────────────────────────────────────
        var recentPayments = paymentJpaRepository.findTopByOrderByProcessedAtDesc(
                PageRequest.of(0, Math.min(limit / 2, 20)));

        for (var p : recentPayments) {
            String label = p.getStatus() == PaymentStatus.APPROVED
                    ? "Payment approved"
                    : "Payment processed";
            String amountStr = p.getAmount() != null
                    ? "$ " + String.format("%,.0f", p.getAmount()) + " " + p.getCurrency()
                    : "";
            items.add(ActivityItemResponse.builder()
                    .type("PAYMENT")
                    .title(label)
                    .description(amountStr)
                    .actor("Order #" + p.getOrderId())
                    .occurredAt(p.getProcessedAt())
                    .build());
        }

        // ── Recent user registrations ─────────────────────────────────────────
        var recentUsers = userJpaRepository.findTopByOrderByCreatedAtDesc(
                PageRequest.of(0, Math.min(limit / 3, 15)));

        for (var u : recentUsers) {
            items.add(ActivityItemResponse.builder()
                    .type("USER_REGISTERED")
                    .title("New user registered")
                    .description("@" + u.getAlias())
                    .actor(u.getRole() != null ? u.getRole().getName() : "USER")
                    .occurredAt(u.getCreatedAt())
                    .build());
        }

        // ── Sort by occurredAt desc, cap to limit ─────────────────────────────
        var sorted = items.stream()
                .filter(i -> i.getOccurredAt() != null)
                .sorted(Comparator.comparing(ActivityItemResponse::getOccurredAt).reversed())
                .limit(limit)
                .toList();

        return ResponseEntity.ok(ApiResponse.ok("Activity feed retrieved", sorted));
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Bulk wallet credit
    // ═════════════════════════════════════════════════════════════════════════

    @Operation(summary = "Credit wallet balance to ALL platform users — idempotent via jobId")
    @PostMapping("/wallet/credit-all")
    public ResponseEntity<ApiResponse<BulkCreditWalletResponse>> bulkCreditAll(
            @Valid @RequestBody BulkCreditWalletRequest request) {
        BulkCreditWalletResponse result = bulkCreditWalletUseCase.execute(
                request.getAmount(),
                request.getReason(),
                request.getJobId()
        );
        return ResponseEntity.ok(ApiResponse.ok("Bulk wallet credit completed", result));
    }
}
