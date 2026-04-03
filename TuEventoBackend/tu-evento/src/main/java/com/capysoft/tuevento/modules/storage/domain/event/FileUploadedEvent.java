package com.capysoft.tuevento.modules.storage.domain.event;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadedEvent {

    private Integer storedFileId;
    private String fileCategory;
    private Integer uploadedBy;
    private String s3Key;
    private String originalFilename;
    private LocalDateTime occurredAt;
}
