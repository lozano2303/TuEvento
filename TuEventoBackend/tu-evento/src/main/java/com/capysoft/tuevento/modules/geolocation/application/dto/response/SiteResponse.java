package com.capysoft.tuevento.modules.geolocation.application.dto.response;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SiteResponse {

    private final Integer siteId;
    private final Integer cityId;
    private final String cityName;
    private final String departmentName;
    private final String name;
    private final String address;
    private final Integer capacity;
    private final BigDecimal latitude;
    private final BigDecimal longitude;
}
