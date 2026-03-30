package com.capysoft.tuevento.modules.security.interfaces.rest;

import com.capysoft.tuevento.modules.security.application.dto.response.OrganizerRequestResponse;
import com.capysoft.tuevento.modules.security.application.port.in.ApproveOrganizerRequestPort;
import com.capysoft.tuevento.modules.security.application.port.in.GetOrganizerRequestsPort;
import com.capysoft.tuevento.modules.security.application.port.in.RejectOrganizerRequestPort;
import com.capysoft.tuevento.shared.interfaces.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
@Tag(name = "Admin", description = "Administration endpoints — requires ADMIN role")
public class AdminController {

    private final GetOrganizerRequestsPort  getOrganizerRequestsPort;
    private final ApproveOrganizerRequestPort approveOrganizerRequestPort;
    private final RejectOrganizerRequestPort  rejectOrganizerRequestPort;

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
}
