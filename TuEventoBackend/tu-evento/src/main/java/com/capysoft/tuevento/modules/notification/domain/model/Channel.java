package com.capysoft.tuevento.modules.notification.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Channel {
    private Long channelId;
    private String name;
    private String description;
    private boolean active;
    private String config;
}
