package com.capysoft.tuevento.modules.category.application.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AssignCategoryRequest {

    @NotNull
    private Integer categoryId;

    @NotNull
    private Integer eventId;
}
