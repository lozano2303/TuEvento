package com.capysoft.geolocation.application.usecase;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.capysoft.geolocation.application.dto.response.SiteResponse;
import com.capysoft.geolocation.application.port.in.GetNearbySitesPort;
import com.capysoft.geolocation.domain.repository.SiteRepository;

import lombok.RequiredArgsConstructor;

@Service
// Lectura en una transacción: la ciudad y el departamento se cargan de forma perezosa (sin open-in-view)
@Transactional(readOnly = true)
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
