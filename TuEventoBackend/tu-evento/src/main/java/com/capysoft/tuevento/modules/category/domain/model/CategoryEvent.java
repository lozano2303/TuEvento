package com.capysoft.tuevento.modules.category.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryEvent {

    private Integer categoryEventId;
    private Integer categoryId;
    private Integer eventId;
}
