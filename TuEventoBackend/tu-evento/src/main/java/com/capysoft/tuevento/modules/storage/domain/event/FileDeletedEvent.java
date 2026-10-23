package com.capysoft.tuevento.modules.storage.domain.event;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileDeletedEvent {

    private Integer storedFileId;
    private String s3Key;
    private Integer deletedBy;
    private LocalDateTime occurredAt;
}
