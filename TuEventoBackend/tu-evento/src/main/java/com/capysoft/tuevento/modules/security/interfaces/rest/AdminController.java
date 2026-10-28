package com.capysoft.tuevento.modules.security.interfaces.rest;

import com.capysoft.tuevento.modules.security.application.dto.request.RevokeOrganizerRoleRequest;
import com.capysoft.tuevento.modules.security.application.dto.response.ActivityItemResponse;
import com.capysoft.tuevento.modules.security.application.dto.response.AdminDashboardStatsResponse;
import com.capysoft.tuevento.modules.security.application.dto.response.AdminPendingCountsResponse;
import com.capysoft.tuevento.modules.security.application.dto.response.AdminUserResponse;
import com.capysoft.tuevento.modules.security.application.dto.response.OrganizerRequestResponse;
import com.capysoft.tuevento.modules.security.application.port.in.ApproveOrganizerRequestPort;
import com.capysoft.tuevento.modules.security.application.port.in.GetAdminPendingCountsPort;
import com.capysoft.tuevento.modules.security.application.port.in.GetOrganizerRequestsPort;
import com.capysoft.tuevento.modules.security.application.port.in.RejectOrganizerRequestPort;
import com.capysoft.tuevento.modules.security.application.port.in.RevokeOrganizerRolePort;
import com.capysoft.tuevento.modules.security.application.port.out.OrganizerDocumentPort.DocumentUrl;
import com.capysoft.tuevento.modules.security.application.port.out.OrganizerDocumentPort;
import com.capysoft.tuevento.modules.security.application.usecase.GetAdminDashboardUseCase;
import com.capysoft.tuevento.modules.security.domain.model.OrganizerPetition;
import com.capysoft.tuevento.modules.security.domain.repository.OrganizerPetitionRepository;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.repository.UserJpaRepository;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import com.capysoft.tuevento.shared.infrastructure.security.SecurityUser;
import com.capysoft.tuevento.shared.interfaces.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
@Tag(name = "Admin", description = "Administration endpoints — requires ADMIN role")
public class AdminController {
    // Endpoints de admin de Identidad: usuarios, solicitudes de organizador y el resumen del panel.
    // Los de otros servicios viven con su dueño, en la misma URL: /admin/events en Catálogo
    // (AdminEventController), /admin/refunds y /admin/wallet en Comercio.


    // ── Organizer requests ────────────────────────────────────────────────────
    private final GetOrganizerRequestsPort    getOrganizerRequestsPort;
    private final ApproveOrganizerRequestPort approveOrganizerRequestPort;
    private final RejectOrganizerRequestPort  rejectOrganizerRequestPort;
    private final GetAdminPendingCountsPort   getAdminPendingCountsPort;
    private final OrganizerDocumentPort       organizerDocumentPort;
    private final OrganizerPetitionRepository organizerPetitionRepository;

    // ── Organizer role management ─────────────────────────────────────────────
    private final RevokeOrganizerRolePort     revokeOrganizerRolePort;

    // ── Dashboard (compone Identidad + Catálogo + Comercio por puertos) ───────
    private final GetAdminDashboardUseCase    getAdminDashboardUseCase;
    private final UserJpaRepository           userJpaRepository;

    // ═════════════════════════════════════════════════════════════════════════
    // Dashboard stats
    // ═════════════════════════════════════════════════════════════════════════

    @Operation(summary = "Get aggregated platform stats for the admin dashboard")
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminDashboardStatsResponse>> getStats() {
        return ResponseEntity.ok(ApiResponse.ok("Dashboard stats retrieved", getAdminDashboardUseCase.getStats()));
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
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal SecurityUser principal) {
        return ResponseEntity.ok(ApiResponse.ok("Organizer requests retrieved",
                getOrganizerRequestsPort.getRequests(status, principal.getUserId())));
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
    public ResponseEntity<ApiResponse<DocumentUrl>> getDocumentUrl(
            @PathVariable Integer id,
            @AuthenticationPrincipal SecurityUser principal) {
        OrganizerPetition petition = organizerPetitionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("PETITION_NOT_FOUND", "Organizer petition not found"));

        if (petition.getStoredFileId() == null) {
            throw new NotFoundException("FILE_NOT_FOUND", "Organizer petition does not have a document");
        }

        return ResponseEntity.ok(ApiResponse.ok("Document URL generated",
                organizerDocumentPort.publicUrl(petition.getStoredFileId(), principal.getUserId(), principal.getRole())));
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Organizer role management endpoints
    // ═════════════════════════════════════════════════════════════════════════

    @Operation(summary = "Revoke the ORGANIZER role from a user (downgrades to USER) with a required reason. " +
               "Cancels their published events, sends in-app + email notification with the reason " +
               "and pushes a WebSocket role-change signal.")
    @DeleteMapping("/organizer-users/{userId}/revoke")
    public ResponseEntity<ApiResponse<Void>> revokeOrganizerRole(
            @PathVariable Integer userId,
            @Valid @RequestBody RevokeOrganizerRoleRequest request,
            @AuthenticationPrincipal SecurityUser principal) {
        revokeOrganizerRolePort.revoke(userId, request.getReason(), principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok("Organizer role revoked successfully"));
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Activity feed
    // ═════════════════════════════════════════════════════════════════════════

    @Operation(summary = "Get unified admin activity feed — combines recent events, payments and user registrations")
    @GetMapping("/activity")
    public ResponseEntity<ApiResponse<List<ActivityItemResponse>>> getActivity(
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(ApiResponse.ok("Activity feed retrieved", getAdminDashboardUseCase.getActivity(limit)));
    }

    // ═════════════════════════════════════════════════════════════════════════
    // User management
    // ═════════════════════════════════════════════════════════════════════════

    @Operation(summary = "List users (newest first). ?q= searches alias, name and email; ?role=USER|ORGANIZER|ADMIN filters.")
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<AdminUserResponse>>> getUsers(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "100") int limit) {
        String pattern = (q == null || q.isBlank()) ? "" : "%" + q.trim().toLowerCase() + "%";
        String roleCode = (role == null || role.isBlank()) ? "" : role.trim().toUpperCase();
        int size = Math.min(Math.max(limit, 1), 200);
        return ResponseEntity.ok(ApiResponse.ok("Users retrieved",
                userJpaRepository.searchForAdmin(pattern, roleCode, PageRequest.of(0, size))));
    }
}
