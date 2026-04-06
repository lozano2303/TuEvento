package com.capysoft.tuevento.modules.geolocation.application.usecase;

import java.util.List;

import org.springframework.stereotype.Service;

import com.capysoft.tuevento.modules.geolocation.application.dto.response.SiteResponse;
import com.capysoft.tuevento.modules.geolocation.application.port.in.GetSitesByDepartmentPort;
import com.capysoft.tuevento.modules.geolocation.domain.repository.SiteRepository;

import lombok.RequiredArgsConstructor;

@Service
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
