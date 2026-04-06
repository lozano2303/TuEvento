package com.capysoft.tuevento.modules.geolocation.application.usecase;

import org.springframework.stereotype.Service;

import com.capysoft.tuevento.modules.geolocation.application.dto.response.SiteResponse;
import com.capysoft.tuevento.modules.geolocation.application.port.in.GetSitePort;
import com.capysoft.tuevento.modules.geolocation.domain.repository.SiteRepository;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

@Service
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
