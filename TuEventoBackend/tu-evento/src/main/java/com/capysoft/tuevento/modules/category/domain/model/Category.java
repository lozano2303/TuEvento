package com.capysoft.tuevento.modules.category.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Category {

    private Long   categoryId;
    private String name;
    private String description;
    private boolean active;
    private boolean visible;

    /** Nullable — null means this is a root category. */
    private Long dadId;
}
