package com.capysoft.geolocation.application.port.in;

import com.capysoft.geolocation.application.dto.response.CityResponse;

public interface GetCityPort {

    CityResponse getCity(Integer cityId);
}
