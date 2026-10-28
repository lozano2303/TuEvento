package com.capysoft.tuevento.modules.security.domain.repository;

/** Registro de auditoría de las revocaciones del rol de organizador. */
public interface OrganizerRoleRevocationRepository {

    /** Guarda la revocación y devuelve su ID. */
    Long record(Integer userId, Integer revokedBy, String reason);
}
