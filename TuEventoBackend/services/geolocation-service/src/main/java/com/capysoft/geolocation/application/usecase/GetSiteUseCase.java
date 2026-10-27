package com.capysoft.geolocation.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.capysoft.geolocation.application.dto.response.SiteResponse;
import com.capysoft.geolocation.application.port.in.GetSitePort;
import com.capysoft.geolocation.domain.repository.SiteRepository;
import com.capysoft.common.domain.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

@Service
// Lectura en una transacción: la ciudad y el departamento se cargan de forma perezosa (sin open-in-view)
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GetSiteUseCase implements GetSitePort {

    private final SiteRepository siteRepository;

    @Override
    public SiteResponse getSite(Integer siteId) {
        return siteRepository.findById(siteId)
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
                .orElseThrow(() -> new NotFoundException("SITE_NOT_FOUND",
                        "Site not found with id: " + siteId));
    }
}
