package com.capysoft.tuevento.modules.security.domain.repository;

/** Registro de auditoría de las revocaciones del rol de organizador. */
public interface OrganizerRoleRevocationRepository {

    void record(Integer userId, Integer revokedBy, String reason);
}
