package com.capysoft.geolocation.application.port.in;

import com.capysoft.geolocation.application.dto.response.SiteResponse;

public interface GetSitePort {

    SiteResponse getSite(Integer siteId);
}
