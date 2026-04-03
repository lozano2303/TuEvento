package com.capysoft.tuevento.modules.storage.infrastructure.persistence.entity;

import com.capysoft.tuevento.shared.infrastructure.persistence.JpaAuditingEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "storage_provider")
public class StorageProviderEntity extends JpaAuditingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "storage_provider_id")
    private Integer storageProviderId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "code", nullable = false, unique = true, length = 20)
    private String code;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "config", nullable = false, columnDefinition = "jsonb")
    private String config;
}
