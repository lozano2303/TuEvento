package com.capysoft.tuevento.modules.theme.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Append-only audit log for theme-related actions.
 * Records are never modified after creation.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThemeLog {

    private Integer id;
    private Integer userThemeId;
    private String action;
    private String property;
    private String oldValue;
    private String newValue;
    private LocalDateTime createdAt;
}
