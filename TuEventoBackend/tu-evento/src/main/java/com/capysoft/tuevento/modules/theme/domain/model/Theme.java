package com.capysoft.tuevento.modules.theme.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Theme {

    private Integer id;
    private String name;
    private String description;

    /** JSON palette stored as raw String — no Jackson dependency in domain. */
    private String defaultPalette;
}
