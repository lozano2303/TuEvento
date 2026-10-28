package com.capysoft.identity.security.interfaces.rest;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.capysoft.identity.security.application.dto.UserSummary;
import com.capysoft.identity.security.application.port.in.GetUserSummariesPort;
import com.capysoft.identity.security.application.port.in.ListUserIdsPort;
import com.capysoft.common.domain.exception.BusinessException;
import com.capysoft.common.interfaces.ApiResponse;

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
    private final ListUserIdsPort      listUserIdsPort;

    /** Total de usuarios de la plataforma (p. ej. para el crédito a todas las carteras). */
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> countUsers() {
        return ResponseEntity.ok(ApiResponse.ok("Users counted", listUserIdsPort.countUsers()));
    }

    /**
     * Hasta {@code limit} IDs mayores que {@code afterUserId}, en orden ascendente (ver
     * {@link ListUserIdsPort#findUserIdsAfter}); con el mismo tope que el resumen.
     */
    @GetMapping("/ids")
    public ResponseEntity<ApiResponse<List<Integer>>> getUserIds(@RequestParam(defaultValue = "0") int afterUserId,
                                                                 @RequestParam(defaultValue = "500") int limit) {
        int bounded = Math.max(1, Math.min(limit, MAX_IDS));
        return ResponseEntity.ok(ApiResponse.ok("User ids retrieved", listUserIdsPort.findUserIdsAfter(afterUserId, bounded)));
    }

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
