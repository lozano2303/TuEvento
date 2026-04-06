package com.capysoft.tuevento.modules.geolocation.domain.model;

import java.math.BigDecimal;

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
public class Site {

    private Integer siteId;
    private City city;
    private String name;
    private String address;
    private Integer capacity;
    private BigDecimal latitude;
    private BigDecimal longitude;
}
