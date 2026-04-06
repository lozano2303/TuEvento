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
public class City {

    private Integer cityId;
    private Department department;
    private String name;
    private String code;
}
