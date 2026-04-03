package com.capysoft.tuevento.modules.storage.domain.model;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageProvider {

    private Integer storageProviderId;
    private String name;
    private String code;
    private Boolean isActive;

    /** JSONB stored as String in the domain layer. */
    private String config;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
