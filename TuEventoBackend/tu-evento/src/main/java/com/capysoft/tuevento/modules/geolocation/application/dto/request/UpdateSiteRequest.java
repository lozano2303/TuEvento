package com.capysoft.tuevento.modules.geolocation.application.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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
public class UpdateSiteRequest {

    @Size(max = 150)
    private String name;

    @Size(max = 200)
    private String address;

    @Positive
    private Integer capacity;

    @DecimalMin(value = "-90.000000")
    @DecimalMax(value = "90.000000")
    private BigDecimal latitude;

    @DecimalMin(value = "-180.000000")
    @DecimalMax(value = "180.000000")
    private BigDecimal longitude;
}
