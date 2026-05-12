package com.capysoft.tuevento.modules.event.domain.model;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventMediaLog {

    private Long mediaLogId;
    private Long eventId;
    private Long mediaId;
    private String imgUrl;
    private int version;
    private Boolean isVisible;
}
