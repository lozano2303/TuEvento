package com.capysoft.tuevento.modules.geolocation.application.usecase;

import org.springframework.stereotype.Service;

import com.capysoft.tuevento.modules.geolocation.application.dto.response.CityResponse;
import com.capysoft.tuevento.modules.geolocation.application.port.in.GetCityPort;
import com.capysoft.tuevento.modules.geolocation.domain.repository.CityRepository;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetCityUseCase implements GetCityPort {

    private final CityRepository cityRepository;

    @Override
    public CityResponse getCity(Integer cityId) {
        return cityRepository.findById(cityId)
                .map(c -> CityResponse.builder()
                        .cityId(c.getCityId())
                        .departmentId(c.getDepartment().getDepartmentId())
                        .departmentName(c.getDepartment().getName())
                        .name(c.getName())
                        .code(c.getCode())
                        .build())
                .orElseThrow(() -> new NotFoundException("CITY_NOT_FOUND",
                        "City not found with id: " + cityId));
    }
}
