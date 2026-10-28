package com.capysoft.tuevento.modules.security.interfaces.rest;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.capysoft.tuevento.modules.security.application.dto.UserSummary;
import com.capysoft.tuevento.modules.security.application.port.in.GetUserSummariesPort;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.interfaces.ApiResponse;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;

/**
 * API interna de Identidad para los demás servicios (no la usan web ni móvil y el gateway no la
 * enruta). Exige la clave entre servicios: ver {@code InternalApiKeyFilter}.
 */
@Hidden
@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    /** Igual que el bloque de consulta del caso de uso: una petición nunca hace más de una consulta. */
    static final int MAX_IDS = 500;

    private final GetUserSummariesPort getUserSummariesPort;

    /**
     * Datos públicos de los usuarios pedidos ({@code ?ids=1,2,3}). Los desconocidos no aparecen;
     * el orden de la respuesta sigue el de los IDs pedidos.
     */
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<List<UserSummary>>> getSummaries(@RequestParam List<Integer> ids) {
        List<Integer> distinct = ids.stream().filter(id -> id != null).distinct().toList();
        if (distinct.isEmpty()) {
            throw new BusinessException("USER_IDS_REQUIRED", "At least one user id is required");
        }
        if (distinct.size() > MAX_IDS) {
            throw new BusinessException("TOO_MANY_USER_IDS", "At most " + MAX_IDS + " user ids per request");
        }
        var found = getUserSummariesPort.getSummaries(distinct);
        List<UserSummary> summaries = distinct.stream().map(found::get).filter(s -> s != null).toList();
        return ResponseEntity.ok(ApiResponse.ok("User summaries retrieved", summaries));
    }
}
