package com.capysoft.tuevento.modules.section.application.dto.response;

import com.capysoft.tuevento.modules.section.domain.model.SectionCategory;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SectionTypeResponse {
    private Integer sectionTypeId;
    private String name;
    private Boolean active;
    private SectionCategory category;
}
