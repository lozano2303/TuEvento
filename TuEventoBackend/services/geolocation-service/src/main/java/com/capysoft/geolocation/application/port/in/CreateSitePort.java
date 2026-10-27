package com.capysoft.geolocation.application.port.in;

import com.capysoft.geolocation.application.dto.request.CreateSiteRequest;
import com.capysoft.geolocation.application.dto.response.SiteResponse;

public interface CreateSitePort {

    SiteResponse createSite(CreateSiteRequest request);
}
