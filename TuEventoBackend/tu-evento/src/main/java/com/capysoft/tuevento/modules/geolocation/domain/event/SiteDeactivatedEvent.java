package com.capysoft.tuevento.modules.geolocation.domain.event;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SiteDeactivatedEvent {

    private Integer siteId;
    private LocalDateTime occurredAt;
}
