package com.capysoft.tuevento.modules.storage.domain.event;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUrlGeneratedEvent {

    private Integer storedFileId;
    private String publicUrl;
    private Integer generatedBy;
    private LocalDateTime occurredAt;
}
