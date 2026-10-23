package com.capysoft.tuevento.modules.theme.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThemeCustomization {

    private Integer id;
    private Integer userThemeId;
    private String property;
    private String value;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
