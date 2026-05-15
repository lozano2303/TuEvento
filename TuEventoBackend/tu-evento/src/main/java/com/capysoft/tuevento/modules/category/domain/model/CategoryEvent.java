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

    private Long categoryEventId;
    private Long categoryId;
    private Long eventId;
}
