package com.capysoft.tuevento.modules.theme.application.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ThemeLogResponse {

    private final Integer logId;
    private final String action;
    private final String property;
    private final String oldValue;
    private final String newValue;
    private final LocalDateTime createdAt;
}
