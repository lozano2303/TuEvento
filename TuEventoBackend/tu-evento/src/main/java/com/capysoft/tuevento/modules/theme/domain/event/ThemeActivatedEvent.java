package com.capysoft.tuevento.modules.theme.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThemeActivatedEvent {

    private Integer userId;
    private Integer themeId;
    private Integer userThemeId;
}
