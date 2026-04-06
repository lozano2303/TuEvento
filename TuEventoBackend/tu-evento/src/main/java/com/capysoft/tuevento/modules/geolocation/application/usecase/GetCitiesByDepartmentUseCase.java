package com.capysoft.tuevento.modules.geolocation.application.usecase;

import java.util.List;

import org.springframework.stereotype.Service;

import com.capysoft.tuevento.modules.geolocation.application.dto.response.CityResponse;
import com.capysoft.tuevento.modules.geolocation.application.port.in.GetCitiesByDepartmentPort;
import com.capysoft.tuevento.modules.geolocation.domain.repository.CityRepository;
import com.capysoft.tuevento.modules.geolocation.domain.repository.DepartmentRepository;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetCitiesByDepartmentUseCase implements GetCitiesByDepartmentPort {

    private final DepartmentRepository departmentRepository;
    private final CityRepository cityRepository;

    @Override
    public List<CityResponse> getCitiesByDepartment(Integer departmentId) {
        departmentRepository.findById(departmentId)
                .orElseThrow(() -> new NotFoundException("DEPARTMENT_NOT_FOUND",
                        "Department not found with id: " + departmentId));

        return cityRepository.findByDepartmentId(departmentId).stream()
                .map(c -> CityResponse.builder()
                        .cityId(c.getCityId())
                        .departmentId(c.getDepartment().getDepartmentId())
                        .departmentName(c.getDepartment().getName())
                        .name(c.getName())
                        .code(c.getCode())
                        .build())
                .toList();
    }
}
