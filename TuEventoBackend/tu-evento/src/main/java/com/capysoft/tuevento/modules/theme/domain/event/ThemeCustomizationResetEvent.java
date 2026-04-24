package com.capysoft.tuevento.modules.theme.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThemeCustomizationResetEvent {

    private Integer userId;
    private Integer userThemeId;
    private String property;
}
