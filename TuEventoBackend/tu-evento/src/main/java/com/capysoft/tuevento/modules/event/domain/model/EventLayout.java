package com.capysoft.tuevento.modules.event.domain.model;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventLayout {

    private Long eventLayoutId;
    private Long eventId;
    private String layoutData; // JSONB stored as String in domain
}
