package com.capysoft.tuevento.modules.storage.infrastructure.persistence.entity;

import com.capysoft.tuevento.shared.infrastructure.persistence.JpaAuditingEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "stored_file")
public class StoredFileEntity extends JpaAuditingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stored_file_id")
    private Integer storedFileId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_category_id", nullable = false)
    private FileCategoryEntity fileCategory;

    /** ID of the uploading user — no JPA relation to avoid cross-module coupling. */
    @Column(name = "uploaded_by", nullable = false)
    private Integer uploadedBy;

    @Column(name = "s3_key", nullable = false, length = 512)
    private String s3Key;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Column(name = "public_url", length = 512)
    private String publicUrl;

    @Column(name = "owner_entity_id")
    private Integer ownerEntityId;

    @Column(name = "owner_entity_type", length = 50)
    private String ownerEntityType;

    @Column(name = "deleted", nullable = false)
    private Boolean deleted;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;
}
