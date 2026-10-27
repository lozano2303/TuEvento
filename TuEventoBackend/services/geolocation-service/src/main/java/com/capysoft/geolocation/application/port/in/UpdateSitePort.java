package com.capysoft.geolocation.application.port.in;

import com.capysoft.geolocation.application.dto.request.UpdateSiteRequest;
import com.capysoft.geolocation.application.dto.response.SiteResponse;

public interface UpdateSitePort {

    SiteResponse updateSite(Integer siteId, UpdateSiteRequest request);
}
