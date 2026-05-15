package com.capysoft.tuevento.modules.category.application.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CategoryResponse {

    private Integer categoryId;
    private String  name;
    private String  description;
    private boolean active;
    private boolean visible;
    private Integer dadId;
}
