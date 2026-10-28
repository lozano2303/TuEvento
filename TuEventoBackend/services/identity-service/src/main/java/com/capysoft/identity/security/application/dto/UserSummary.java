package com.capysoft.identity.security.application.dto;

/**
 * Datos públicos de un usuario que otros servicios necesitan para mostrarlo o avisarle.
 * Es el contrato de Identidad hacia fuera: hoy se entrega dentro del monolito y al separar el
 * servicio será la respuesta de {@code GET /internal/users/summary}.
 *
 * @param fullName            nombre del perfil; {@code null} si el usuario aún no tiene perfil
 * @param storedFileId        foto de perfil en Archivos; {@code null} si no tiene
 * @param preferredLanguageId idioma preferido; {@code null} si no eligió ninguno
 */
public record UserSummary(
        Integer userId,
        String alias,
        String email,
        String fullName,
        Integer storedFileId,
        Long preferredLanguageId) {
}
