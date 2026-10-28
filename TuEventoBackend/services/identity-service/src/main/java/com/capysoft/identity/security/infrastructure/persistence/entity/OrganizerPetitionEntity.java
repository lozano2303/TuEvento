package com.capysoft.identity.security.infrastructure.persistence.entity;

import com.capysoft.common.infrastructure.persistence.JpaAuditingEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "organizer_petition")
public class OrganizerPetitionEntity extends JpaAuditingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "organizer_petition_id")
    private Integer organizerPetitionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    /** Documento en Archivos; solo el ID, sin relación JPA con sus tablas. */
    @Column(name = "stored_file_id")
    private Integer storedFileId;

    @Column(name = "application_date", nullable = false)
    private LocalDateTime applicationDate;

    @Column(name = "document_type", length = 50)
    private String documentType;

    @Column(name = "status", nullable = false, length = 20)
    private String status;
}
