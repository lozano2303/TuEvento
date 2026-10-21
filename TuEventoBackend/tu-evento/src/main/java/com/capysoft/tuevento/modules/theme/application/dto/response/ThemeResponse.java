package com.capysoft.tuevento.modules.theme.application.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ThemeResponse {

    private final Integer id;
    private final String name;
    private final String description;
    
    // Translation fields (optional - only present when language parameter is provided)
    private final String translatedName;
    private final String translatedDescription;
    private final String languageCode;
}
