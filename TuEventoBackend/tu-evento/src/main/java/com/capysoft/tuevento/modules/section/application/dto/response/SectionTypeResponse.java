package com.capysoft.tuevento.modules.section.application.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SectionTypeResponse {
    private Integer sectionTypeId;
    private String name;
}
