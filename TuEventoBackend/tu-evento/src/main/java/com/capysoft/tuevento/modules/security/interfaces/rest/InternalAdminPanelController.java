package com.capysoft.tuevento.modules.security.interfaces.rest;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.capysoft.tuevento.modules.security.application.port.out.AdminPanelSourcesPort;
import com.capysoft.tuevento.shared.interfaces.ApiResponse;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;

/**
 * API interna para identity-service, que ya atiende el panel de admin: los datos de Catálogo y
 * Comercio siguen aquí y se leen por el mismo puerto que usaba el panel. No la usan web ni móvil y
 * el gateway no la enruta; exige la clave entre servicios ({@code InternalApiKeyFilter}).
 */
@Hidden
@RestController
@RequestMapping("/internal/admin-panel")
@RequiredArgsConstructor
public class InternalAdminPanelController {

    /** Igual que el panel: nunca más de 100 elementos de actividad por fuente. */
    static final int MAX_LIMIT = 100;

    private final AdminPanelSourcesPort sources;

    @GetMapping("/catalog-stats")
    public ResponseEntity<ApiResponse<AdminPanelSourcesPort.CatalogStats>> catalogStats() {
        return ResponseEntity.ok(ApiResponse.ok("Catalog stats retrieved", sources.catalogStats()));
    }

    @GetMapping("/events-pending-review")
    public ResponseEntity<ApiResponse<Long>> eventsPendingReview() {
        return ResponseEntity.ok(ApiResponse.ok("Events pending review counted", sources.eventsPendingReview()));
    }

    @GetMapping("/recent-events")
    public ResponseEntity<ApiResponse<List<AdminPanelSourcesPort.RecentEvent>>> recentEvents(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(ApiResponse.ok("Recent events retrieved", sources.recentlyUpdatedEvents(clamp(limit))));
    }

    @GetMapping("/commerce-stats")
    public ResponseEntity<ApiResponse<AdminPanelSourcesPort.CommerceStats>> commerceStats() {
        return ResponseEntity.ok(ApiResponse.ok("Commerce stats retrieved", sources.commerceStats()));
    }

    @GetMapping("/recent-payments")
    public ResponseEntity<ApiResponse<List<AdminPanelSourcesPort.RecentPayment>>> recentPayments(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(ApiResponse.ok("Recent payments retrieved", sources.recentPayments(clamp(limit))));
    }

    private static int clamp(int limit) {
        return Math.max(1, Math.min(limit, MAX_LIMIT));
    }
}
