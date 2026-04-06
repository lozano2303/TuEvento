package com.capysoft.tuevento.modules.geolocation.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Department {

    private Integer departmentId;
    private String name;
    private String code;
}
