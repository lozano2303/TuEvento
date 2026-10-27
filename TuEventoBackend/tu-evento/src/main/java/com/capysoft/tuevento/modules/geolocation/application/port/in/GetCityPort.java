package com.capysoft.tuevento.modules.geolocation.application.port.in;

import com.capysoft.tuevento.modules.geolocation.application.dto.response.CityResponse;

public interface GetCityPort {

    CityResponse getCity(Integer cityId);
}
