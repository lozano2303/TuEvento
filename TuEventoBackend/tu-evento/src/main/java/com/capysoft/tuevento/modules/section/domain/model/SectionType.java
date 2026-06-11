package com.capysoft.tuevento.modules.section.domain.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SectionType {
    private Integer sectionTypeId;
    private String name;
}
