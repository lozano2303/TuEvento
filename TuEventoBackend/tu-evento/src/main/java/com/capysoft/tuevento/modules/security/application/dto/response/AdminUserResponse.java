package com.capysoft.tuevento.modules.security.application.dto.response;

import java.time.LocalDateTime;

/**
 * Fila del listado de usuarios del panel admin (GET /admin/users).
 * Proyección de Spring Data: los alias de la consulta JPQL deben coincidir con los getters.
 */
public interface AdminUserResponse {
    Integer getUserId();
    String getAlias();
    String getFullName();
    String getEmail();
    String getRole();
    String getStatus();
    LocalDateTime getCreatedAt();
}
