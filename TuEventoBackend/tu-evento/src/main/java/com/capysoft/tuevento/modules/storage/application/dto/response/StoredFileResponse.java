package com.capysoft.tuevento.modules.storage.application.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class StoredFileResponse {

    private Integer storedFileId;
    private String fileCategoryCode;
    private Integer uploadedBy;
    private String s3Key;
    private String originalFilename;
    private String contentType;
    private Long sizeBytes;
    private String publicUrl;
    private Integer ownerEntityId;
    private String ownerEntityType;
    private LocalDateTime uploadedAt;
}
