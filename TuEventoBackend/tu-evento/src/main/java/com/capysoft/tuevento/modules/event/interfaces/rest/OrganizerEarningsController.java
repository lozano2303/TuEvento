package com.capysoft.tuevento.modules.event.interfaces.rest;

import com.capysoft.tuevento.modules.event.application.dto.response.OrganizerEarningsResponse;
import com.capysoft.tuevento.modules.event.application.usecase.GetOrganizerEarningsService;
import com.capysoft.tuevento.shared.infrastructure.security.SecurityUser;
import com.capysoft.tuevento.shared.interfaces.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint para el panel de ganancias del organizador.
 *
 * GET /api/v1/organizer/earnings
 *   — Solo accesible por ORGANIZER (o ADMIN).
 *   — Retorna el total recaudado y el desglose por evento.
 */
@RestController
@RequestMapping("/api/v1/organizer")
@RequiredArgsConstructor
public class OrganizerEarningsController {

    private final GetOrganizerEarningsService getOrganizerEarningsService;

    @GetMapping("/earnings")
    @PreAuthorize("hasAnyAuthority('ORGANIZER', 'ADMIN')")
    public ResponseEntity<ApiResponse<OrganizerEarningsResponse>> getEarnings(
            @AuthenticationPrincipal SecurityUser securityUser) {
        Long organizerId = securityUser.getUserId().longValue();
        OrganizerEarningsResponse response = getOrganizerEarningsService.execute(organizerId);
        return ResponseEntity.ok(ApiResponse.ok("Earnings retrieved successfully", response));
    }
}
