package com.capysoft.tuevento.modules.event.domain.event;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventMediaUploadedEvent {

    private Long mediaId;
    private Long eventId;
    private String imgUrl;
    private LocalDateTime occurredAt;
}
