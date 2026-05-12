package com.capysoft.tuevento.modules.event.domain.model;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventMedia {

    private Long mediaId;
    private Long eventId;
    private String imgUrl;
}
