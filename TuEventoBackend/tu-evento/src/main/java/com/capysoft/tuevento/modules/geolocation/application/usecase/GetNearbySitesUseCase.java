package com.capysoft.tuevento.modules.geolocation.application.usecase;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import com.capysoft.tuevento.modules.geolocation.application.dto.response.SiteResponse;
import com.capysoft.tuevento.modules.geolocation.application.port.in.GetNearbySitesPort;
import com.capysoft.tuevento.modules.geolocation.domain.repository.SiteRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetNearbySitesUseCase implements GetNearbySitesPort {

    private final SiteRepository siteRepository;

    @Override
    public List<SiteResponse> getNearbySites(BigDecimal latitude, BigDecimal longitude, double radiusKm) {
        return siteRepository.findNearby(latitude, longitude, radiusKm).stream()
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
