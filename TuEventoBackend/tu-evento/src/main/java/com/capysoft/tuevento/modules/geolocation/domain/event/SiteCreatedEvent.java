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
public class SiteCreatedEvent {

    private Integer siteId;
    private String name;
    private Integer cityId;
    private LocalDateTime occurredAt;
}
