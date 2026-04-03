package com.capysoft.tuevento.modules.storage.domain.model;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileCategory {

    private Integer fileCategoryId;
    private StorageProvider storageProvider;
    private String name;
    private String code;
    private Boolean isPublic;
    private String allowedExtensions;
    private Integer maxSizeMb;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
