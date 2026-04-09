package com.capysoft.tuevento.modules.geolocation.application.usecase;

import java.time.LocalDateTime;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.capysoft.tuevento.modules.geolocation.application.dto.request.UpdateSiteRequest;
import com.capysoft.tuevento.modules.geolocation.application.dto.response.SiteResponse;
import com.capysoft.tuevento.modules.geolocation.application.port.in.UpdateSitePort;
import com.capysoft.tuevento.modules.geolocation.domain.event.SiteUpdatedEvent;
import com.capysoft.tuevento.modules.geolocation.domain.model.Site;
import com.capysoft.tuevento.modules.geolocation.domain.repository.SiteRepository;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UpdateSiteUseCase implements UpdateSitePort {

    private final SiteRepository siteRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public SiteResponse updateSite(Integer siteId, UpdateSiteRequest request) {
        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new NotFoundException("SITE_NOT_FOUND",
                        "Site not found with id: " + siteId));

        if (request.getName() != null)      site.setName(request.getName());
        if (request.getAddress() != null)   site.setAddress(request.getAddress());
        if (request.getCapacity() != null)  site.setCapacity(request.getCapacity());
        if (request.getLatitude() != null)  site.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) site.setLongitude(request.getLongitude());

        Site saved = siteRepository.save(site);

        eventPublisher.publishEvent(SiteUpdatedEvent.builder()
                .siteId(saved.getSiteId())
                .name(saved.getName())
                .occurredAt(LocalDateTime.now())
                .build());

        return SiteResponse.builder()
                .siteId(saved.getSiteId())
                .cityId(saved.getCity().getCityId())
                .cityName(saved.getCity().getName())
                .departmentName(saved.getCity().getDepartment().getName())
                .name(saved.getName())
                .address(saved.getAddress())
                .capacity(saved.getCapacity())
                .latitude(saved.getLatitude())
                .longitude(saved.getLongitude())
                .build();
    }
}
