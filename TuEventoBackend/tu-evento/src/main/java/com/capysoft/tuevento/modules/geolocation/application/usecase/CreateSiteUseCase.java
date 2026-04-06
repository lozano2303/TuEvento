package com.capysoft.tuevento.modules.geolocation.application.usecase;

import java.time.LocalDateTime;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.capysoft.tuevento.modules.geolocation.application.dto.request.CreateSiteRequest;
import com.capysoft.tuevento.modules.geolocation.application.dto.response.SiteResponse;
import com.capysoft.tuevento.modules.geolocation.application.port.in.CreateSitePort;
import com.capysoft.tuevento.modules.geolocation.domain.event.SiteCreatedEvent;
import com.capysoft.tuevento.modules.geolocation.domain.model.City;
import com.capysoft.tuevento.modules.geolocation.domain.model.Site;
import com.capysoft.tuevento.modules.geolocation.domain.repository.CityRepository;
import com.capysoft.tuevento.modules.geolocation.domain.repository.SiteRepository;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateSiteUseCase implements CreateSitePort {

    private final SiteRepository siteRepository;
    private final CityRepository cityRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public SiteResponse createSite(CreateSiteRequest request) {
        City city = cityRepository.findById(request.getCityId())
                .orElseThrow(() -> new NotFoundException("CITY_NOT_FOUND",
                        "City not found with id: " + request.getCityId()));

        Site saved = siteRepository.save(Site.builder()
                .city(city)
                .name(request.getName())
                .address(request.getAddress())
                .capacity(request.getCapacity())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .build());

        eventPublisher.publishEvent(SiteCreatedEvent.builder()
                .siteId(saved.getSiteId())
                .name(saved.getName())
                .cityId(city.getCityId())
                .occurredAt(LocalDateTime.now())
                .build());

        return toResponse(saved);
    }

    private SiteResponse toResponse(Site s) {
        return SiteResponse.builder()
                .siteId(s.getSiteId())
                .cityId(s.getCity().getCityId())
                .cityName(s.getCity().getName())
                .departmentName(s.getCity().getDepartment().getName())
                .name(s.getName())
                .address(s.getAddress())
                .capacity(s.getCapacity())
                .latitude(s.getLatitude())
                .longitude(s.getLongitude())
                .build();
    }
}
