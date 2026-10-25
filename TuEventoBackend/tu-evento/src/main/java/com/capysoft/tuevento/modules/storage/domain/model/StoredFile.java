package com.capysoft.tuevento.modules.storage.domain.model;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoredFile {

    private Integer storedFileId;
    private FileCategory fileCategory;

    /** ID of the user who uploaded the file. */
    private Integer uploadedBy;

    private String s3Key;
    private String originalFilename;
    private String contentType;
    private Long sizeBytes;
    private String publicUrl;

    /** ID of the entity that owns this file (polymorphic reference). */
    private Integer ownerEntityId;

    /** Type of the owning entity (e.g. "EVENT", "USER", "PROFILE"). */
    private String ownerEntityType;

    private Boolean deleted;
    private LocalDateTime uploadedAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
