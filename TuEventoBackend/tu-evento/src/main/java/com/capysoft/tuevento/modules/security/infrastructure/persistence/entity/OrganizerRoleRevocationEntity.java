package com.capysoft.tuevento.modules.security.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Auditoría de revocaciones del rol de organizador (tabla organizer_role_revocation). */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "organizer_role_revocation")
public class OrganizerRoleRevocationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "revocation_id")
    private Long revocationId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "revoked_by", nullable = false)
    private Integer revokedBy;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
