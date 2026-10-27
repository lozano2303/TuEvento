package com.capysoft.geolocation.application.usecase;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.capysoft.geolocation.application.dto.response.SiteResponse;
import com.capysoft.geolocation.application.port.in.GetSitesByDepartmentPort;
import com.capysoft.geolocation.domain.repository.SiteRepository;

import lombok.RequiredArgsConstructor;

@Service
// Lectura en una transacción: la ciudad y el departamento se cargan de forma perezosa (sin open-in-view)
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GetSitesByDepartmentUseCase implements GetSitesByDepartmentPort {

    private final SiteRepository siteRepository;

    @Override
    public List<SiteResponse> getSitesByDepartment(Integer departmentId) {
        return siteRepository.findByDepartmentId(departmentId).stream()
                .map(s -> SiteResponse.builder()
                        .siteId(s.getSiteId())
                        .cityId(s.getCity().getCityId())
                        .cityName(s.getCity().getName())
                        .departmentName(s.getCity().getDepartment().getName())
                        .name(s.getName())
                        .address(s.getAddress())
                        .capacity(s.getCapacity())
                        .latitude(s.getLatitude())
                        .longitude(s.getLongitude())
                        .build())
                .toList();
    }
}
