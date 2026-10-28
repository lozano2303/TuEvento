package com.capysoft.geolocation.application.port.in;

import java.util.List;

import com.capysoft.geolocation.application.dto.response.SiteResponse;

public interface GetSitesByCityPort {

    List<SiteResponse> getSitesByCity(Integer cityId);
}
