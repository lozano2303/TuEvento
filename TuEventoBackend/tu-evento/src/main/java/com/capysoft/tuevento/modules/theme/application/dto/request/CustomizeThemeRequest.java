package com.capysoft.tuevento.modules.theme.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CustomizeThemeRequest {

    @NotBlank
    private String property;

    @NotBlank
    @Pattern(
        regexp = "^#([A-Fa-f0-9]{3,4}|[A-Fa-f0-9]{6}|[A-Fa-f0-9]{8})$|^rgba?\\(.*\\)$",
        message = "value must be a valid hex color (#RGB, #RGBA, #RRGGBB, #RRGGBBAA) or rgb/rgba function"
    )
    private String value;
}
