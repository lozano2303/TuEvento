package com.capysoft.identity.security.application.dto;

/**
 * Payload JSON de {@code OrganizerRoleRevoked} en el topic {@code tuevento.user} (key = userId).
 * Es contrato público de Identidad: añadir campos es compatible; quitar o renombrar no.
 *
 * @param revocationId ID de {@code organizer_role_revocation}; identifica la revocación aunque el
 *                     mismo usuario vuelva a ser organizador y se le revoque otra vez
 * @param occurredAt   instante ISO-8601 en UTC
 */
public record OrganizerRoleRevokedMessage(
        Long revocationId,
        Integer userId,
        String alias,
        Integer revokedBy,
        String reason,
        String occurredAt) {

    public static final String AGGREGATE_TYPE = "user";
    public static final String EVENT_TYPE = "OrganizerRoleRevoked";
}
