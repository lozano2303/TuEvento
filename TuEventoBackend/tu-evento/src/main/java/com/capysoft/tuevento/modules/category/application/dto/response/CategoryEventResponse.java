package com.capysoft.tuevento.modules.category.application.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CategoryEventResponse {

    private Integer categoryEventId;
    private Integer categoryId;
    private Integer eventId;
    private String  categoryName;
}
