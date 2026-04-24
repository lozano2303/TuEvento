package com.capysoft.tuevento.modules.theme.application.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class ResolvedPaletteResponse {

    private final Integer themeId;
    private final String themeName;
    private final Integer userThemeId;

    /** Final resolved palette: base defaultPalette overridden by user customizations. */
    private final Map<String, Object> palette;
}
