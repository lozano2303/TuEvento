package com.capysoft.geolocation.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.capysoft.geolocation.application.dto.response.CityResponse;
import com.capysoft.geolocation.application.port.in.GetCityPort;
import com.capysoft.geolocation.domain.repository.CityRepository;
import com.capysoft.common.domain.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

@Service
// Lectura en una transacción: el departamento se carga de forma perezosa (sin open-in-view)
@Transactional(readOnly = true)
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
