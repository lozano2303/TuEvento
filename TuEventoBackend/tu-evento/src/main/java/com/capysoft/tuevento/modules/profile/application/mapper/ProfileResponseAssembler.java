package com.capysoft.tuevento.modules.profile.application.mapper;

import org.springframework.stereotype.Component;

import com.capysoft.tuevento.modules.profile.application.dto.response.ProfileResponse;
import com.capysoft.tuevento.modules.profile.application.port.out.CityLookupPort;
import com.capysoft.tuevento.modules.profile.application.port.out.CitySummary;
import com.capysoft.tuevento.modules.profile.domain.model.Profile;

import lombok.RequiredArgsConstructor;

/**
 * Arma la parte común de {@link ProfileResponse}: datos del perfil y la ciudad resuelta en el
 * servicio de localización. Si la ciudad no se puede resolver, el perfil se devuelve con el
 * {@code cityId} y sin nombres. Cada caso de uso añade sus campos propios al builder.
 */
@Component
@RequiredArgsConstructor
public class ProfileResponseAssembler {

    private final CityLookupPort cityLookupPort;

    public ProfileResponse.ProfileResponseBuilder builder(Profile p) {
        CitySummary city = cityLookupPort.findCity(p.getCityId()).orElse(null);
        return ProfileResponse.builder()
                .profileId(p.getProfileId())
                .userId(p.getUserId())
                .cityId(p.getCityId())
                .cityName(city != null ? city.name() : null)
                .departmentName(city != null ? city.departmentName() : null)
                .storedFileId(p.getStoredFileId())
                .fullName(p.getFullName())
                .bio(p.getBio());
    }
}
