package com.capysoft.tuevento.modules.category.application.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CategoryEventResponse {

    private Long   categoryEventId;
    private Long   categoryId;
    private Long   eventId;
    private String categoryName;
}
