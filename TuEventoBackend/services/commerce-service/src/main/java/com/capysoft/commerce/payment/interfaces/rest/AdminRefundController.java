package com.capysoft.commerce.payment.interfaces.rest;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.capysoft.commerce.payment.application.dto.response.AdminRefundResponse;
import com.capysoft.commerce.payment.application.port.in.GetCommerceAdminStatsPort;
import com.capysoft.common.interfaces.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** Reembolsos para el panel de administración. Vive en Comercio; antes estaba en el AdminController de Identidad. */
@RestController
@RequestMapping("/api/v1/admin/refunds")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
@Tag(name = "Admin", description = "Administration endpoints — requires ADMIN role")
public class AdminRefundController {

    private final GetCommerceAdminStatsPort getCommerceAdminStatsPort;

    @Operation(summary = "List the most recent refunds with payment breakdown, buyer and event")
    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminRefundResponse>>> getRefunds(
            @RequestParam(defaultValue = "100") int limit) {
        int size = Math.min(Math.max(limit, 1), 300);
        return ResponseEntity.ok(ApiResponse.ok("Refunds retrieved",
                getCommerceAdminStatsPort.findRecentRefunds(size)));
    }
}
