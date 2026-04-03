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
@Table(name = "file_category")
public class FileCategoryEntity extends JpaAuditingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_category_id")
    private Integer fileCategoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "storage_provider_id", nullable = false)
    private StorageProviderEntity storageProvider;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "is_public", nullable = false)
    private Boolean isPublic;

    @Column(name = "allowed_extensions", nullable = false, length = 100)
    private String allowedExtensions;

    @Column(name = "max_size_mb", nullable = false)
    private Integer maxSizeMb;
}
