package com.capysoft.tuevento.modules.theme.application.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ThemeResponse {

    private final Integer id;
    private final String name;
    private final String description;
}
