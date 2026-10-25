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

    /**
     * Archivos de categorías públicas (foto de perfil, media de eventos) los ve
     * cualquier usuario autenticado; los privados (documentos de organizador)
     * solo quien los subió o un administrador.
     */
    public boolean isReadableBy(Integer userId, boolean admin) {
        boolean publicCategory = fileCategory != null && Boolean.TRUE.equals(fileCategory.getIsPublic());
        return publicCategory || isDeletableBy(userId, admin);
    }

    /** Solo quien subió el archivo o un administrador pueden borrarlo. */
    public boolean isDeletableBy(Integer userId, boolean admin) {
        return admin || (userId != null && userId.equals(uploadedBy));
    }
}
