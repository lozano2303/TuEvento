package com.capysoft.tuevento.modules.geolocation.application.usecase;

import java.util.List;

import org.springframework.stereotype.Service;

import com.capysoft.tuevento.modules.geolocation.application.dto.response.DepartmentResponse;
import com.capysoft.tuevento.modules.geolocation.application.port.in.GetDepartmentsPort;
import com.capysoft.tuevento.modules.geolocation.domain.repository.DepartmentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetDepartmentsUseCase implements GetDepartmentsPort {

    private final DepartmentRepository departmentRepository;

    @Override
    public List<DepartmentResponse> getDepartments() {
        return departmentRepository.findAll().stream()
                .map(d -> DepartmentResponse.builder()
                        .departmentId(d.getDepartmentId())
                        .name(d.getName())
                        .code(d.getCode())
                        .build())
                .toList();
    }
}
