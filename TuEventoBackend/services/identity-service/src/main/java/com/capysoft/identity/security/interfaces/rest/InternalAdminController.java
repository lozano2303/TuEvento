package com.capysoft.identity.security.interfaces.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.capysoft.common.interfaces.ApiResponse;
import com.capysoft.identity.security.domain.repository.OrganizerPetitionRepository;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;

/**
 * API interna del panel para el monolito: publica en {@code /topic/admin/pending-counts} los
 * contadores de pendientes y necesita las solicitudes de organizador sin revisar. Exige la clave
 * entre servicios.
 */
@Hidden
@RestController
@RequestMapping("/internal/admin")
@RequiredArgsConstructor
public class InternalAdminController {

    static final String PENDING_STATUS = "PENDING";

    private final OrganizerPetitionRepository organizerPetitionRepository;

    @GetMapping("/organizer-requests/pending-count")
    public ResponseEntity<ApiResponse<Long>> pendingOrganizerRequests() {
        return ResponseEntity.ok(ApiResponse.ok("Pending organizer requests counted",
                (long) organizerPetitionRepository.findByStatus(PENDING_STATUS).size()));
    }
}
